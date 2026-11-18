"""Run the packaged CLI, inventory and imaging against a fresh local MySQL test database."""
import base64
from contextlib import contextmanager
import errno
from http.cookiejar import CookieJar
from pathlib import Path
import json
import os
import pty
import re
import select
import socket
import subprocess
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import HTTPCookieProcessor, ProxyHandler, Request, build_opener
from uuid import uuid4


ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / "inventory/target/inventory-1.0.0.jar"
IMAGING = ROOT / "imaging/target/imaging-1.0.0.jar"
JAVA = str(Path(os.environ["JAVA_HOME"]) / "bin/java") if "JAVA_HOME" in os.environ else "java"
FIXTURE_JPEG = base64.b64decode(
    "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRof"
    "Hh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwh"
    "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAAR"
    "CAADAAQDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAP/xAAUEAEAAAAAAAAAAAAAAAA"
    "AAAAA/8QAFAEBAAAAAAAAAAAAAAAAAAAABf/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAMAwEAA"
    "hEDEQA/AJAGBb//2Q=="
)


def check(condition, message):
    if not condition:
        raise AssertionError(message)


@contextmanager
def child(command, **kwargs):
    process = subprocess.Popen(command, **kwargs)
    try:
        yield process
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=12)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


def first_account(env, directory, username, password):
    """Give the real CLI a terminal so System.console() reads the generated password."""
    master, slave = pty.openpty()
    try:
        with child(
            [JAVA, "-Dfile.encoding=UTF-8", "-jar", str(INVENTORY), "account", "create", username],
            cwd=directory, env=env, stdin=slave, stdout=slave, stderr=slave,
        ) as process:
            os.close(slave)
            slave = None
            output = b""
            pending = ["Jelszó: ".encode(), "Jelszó újra: ".encode()]
            search_from = 0
            deadline = time.monotonic() + 75
            while time.monotonic() < deadline:
                if not select.select([master], [], [], 0.2)[0]:
                    continue
                try:
                    chunk = os.read(master, 8192)
                except OSError as ex:
                    if ex.errno != errno.EIO:  # Linux reports PTY closure as EIO.
                        raise
                    break
                if not chunk:
                    break
                output += chunk
                while pending and (offset := output.find(pending[0], search_from)) != -1:
                    search_from = offset + len(pending.pop(0))
                    os.write(master, password.encode() + b"\n")
            else:
                raise TimeoutError("First-account CLI did not finish.")
            result = process.wait(timeout=5)
            check(password.encode() not in output, "The CLI echoed the generated password.")
            if result or pending:
                raise AssertionError("The first-account CLI failed:\n" + output.decode(errors="replace")[-4000:])
    finally:
        os.close(master)
        if slave is not None:
            os.close(slave)


def free_port():
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        return listener.getsockname()[1]


def request(client, origin, path, *, data=None, headers=None, expected=200):
    req = Request(origin + path, data=data, headers=headers or {})
    try:
        response = client.open(req, timeout=5)
    except HTTPError as error:
        response = error
    with response:
        check(response.status == expected, f"{req.get_method()} {path}: expected {expected}, got {response.status}")
        return response.read()


def login(client, origin, username, password):
    token = json.loads(request(client, origin, "/auth/csrf"))
    request(
        client, origin, "/login",
        data=urlencode({"username": username, "password": password}).encode(),
        headers={"Accept": "application/json", token["headerName"]: token["token"]},
        expected=204,
    )
    session = json.loads(request(client, origin, "/api/session"))
    check(session["username"] == username, "The session does not belong to the newly created account.")
    return {
        session["headerName"]: session["token"],
        "X-Otthontar-User": session["id"],
        "X-Otthontar-Request": "1",
    }


@contextmanager
def service(jar, port, directory, env):
    origin = f"http://127.0.0.1:{port}"
    with tempfile.TemporaryFile() as log, child(
        [JAVA, "-Xmx256m", "-Djava.awt.headless=true", "-jar", str(jar), f"--server.port={port}"],
        cwd=directory, env=env, stdout=log, stderr=subprocess.STDOUT,
    ) as process:
        probe = build_opener(ProxyHandler({}))
        try:
            healthy = False
            for _ in range(150):
                if process.poll() is not None:
                    break
                try:
                    with probe.open(origin + "/actuator/health", timeout=1) as response:
                        healthy = json.load(response)["status"] == "UP"
                except (URLError, TimeoutError):
                    pass
                if healthy:
                    break
                time.sleep(0.2)
            if not healthy:
                raise RuntimeError(f"{jar.name} did not become healthy.")
            yield origin
        except Exception:
            log.seek(max(0, log.seek(0, os.SEEK_END) - 6000))
            print(f"{jar.name} log:\n{log.read().decode(errors='replace')}", flush=True)
            raise


def save_fixture(client, origin, headers):
    request(client, origin, "/api/places", data=b'{"name":"Smoke test location"}',
            headers={**headers, "Content-Type": "application/json"})
    places = json.loads(request(client, origin, "/api/places"))
    item_id = str(uuid4())
    draft = json.dumps({
        "id": item_id, "version": 0, "name": "Smoke test item", "description": "",
        "location": places[0]["id"], "status": "KEEP", "cover": "new:0", "actionId": "KEEP",
    }).encode()
    boundary = uuid4().hex
    body = (
        f'--{boundary}\r\nContent-Disposition: form-data; name="draft"\r\n'
        'Content-Type: application/json\r\n\r\n'
    ).encode() + draft + (
        f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="files"; filename="fixture.jpg"\r\n'
        'Content-Type: image/jpeg\r\n\r\n'
    ).encode() + FIXTURE_JPEG + f"\r\n--{boundary}--\r\n".encode()
    saved = json.loads(request(
        client, origin, "/api/items", data=body, headers={
            **headers, "Content-Type": f"multipart/form-data; boundary={boundary}",
            "X-Otthontar-Save-Id": str(uuid4()),
        },
    ))
    check(saved["id"] == item_id and len(saved["photos"]) == 1, "The photo save did not succeed.")
    return item_id, saved["cover"]


def main():
    # Only the explicitly selected local fixture schema is reachable from either JAR.
    db_url = os.environ["OTTHONTAR_SMOKE_DB_URL"]
    check(re.fullmatch(r"jdbc:mysql://127\.0\.0\.1:[0-9]+/inventory_jpa_test_cli", db_url),
          "Use a fresh loopback inventory_jpa_test_cli database.")
    env = {
        name: os.environ[name] for name in ("PATH", "LANG", "LC_ALL") if name in os.environ
    }
    env.update(
        SPRING_DATASOURCE_URL=db_url,
        SPRING_DATASOURCE_USERNAME=os.environ["OTTHONTAR_SMOKE_DB_USER"],
        SPRING_DATASOURCE_PASSWORD=os.environ["OTTHONTAR_SMOKE_DB_PASSWORD"],
        SPRING_MAIN_BANNER_MODE="off",
        LOGGING_LEVEL_ROOT="WARN",
        SESSION_COOKIE_SECURE="false",
    )
    check(INVENTORY.is_file() and IMAGING.is_file(), "Package both JARs before running the smoke test.")
    username = "smoke-" + uuid4().hex[:12]
    password = uuid4().hex
    with tempfile.TemporaryDirectory(prefix="otthontar-smoke-") as directory:
        first_account(env, directory, username, password)
        print("OK: created the first account with the packaged CLI and hidden terminal input")
        with service(IMAGING, free_port(), directory, env) as imaging:
            web_env = {**env, "IMAGING_URL": imaging, "PHOTO_DIR": str(Path(directory) / "photos")}
            port = free_port()
            with service(INVENTORY, port, directory, web_env) as origin:
                client = build_opener(ProxyHandler({}), HTTPCookieProcessor(CookieJar()))
                request(client, origin, "/api/items", expected=401)
                headers = login(client, origin, username, password)
                item_id, photo_id = save_fixture(client, origin, headers)
                check(request(client, origin, f"/api/photos/{photo_id}/small").startswith(b"\xff\xd8"),
                      "Imaging did not produce a JPEG preview.")
            with service(INVENTORY, port, directory, web_env) as origin:
                request(client, origin, "/api/items", expected=401)
                login(client, origin, username, password)
                saved = json.loads(request(client, origin, f"/api/items/{item_id}"))
                check(saved["cover"] == photo_id, "The cover did not persist across the restart.")
                check(request(client, origin, f"/api/photos/{photo_id}/original") == FIXTURE_JPEG,
                      "The original photo did not persist across the restart.")
        print("OK: real HTTP login, photo upload, thumbnail and restart persistence")


if __name__ == "__main__":
    main()
