#!/usr/bin/env bash
# Fixture tests: no real MySQL, systemd changes, or encryption keys are used.
set -Eeuo pipefail
repo=$(cd -- "$(dirname -- "$0")/../.." && pwd -P)
tmp=$(mktemp -d)
trap 'rm -rf -- "$tmp"' EXIT
mkdir -p "$tmp/bin" "$tmp/photos" "$tmp/backups"
chmod 700 "$tmp/backups"
printf 'sample-photo' > "$tmp/photos/image.jpg"
printf 'local-secret-config' > "$tmp/local.conf"
printf '[client]\nuser=fixture\npassword=fixture\n' > "$tmp/mysql.cnf"
chmod 600 "$tmp/mysql.cnf" "$tmp/local.conf"
cat > "$tmp/bin/systemctl" <<'MOCK'
#!/usr/bin/env bash
echo "$*" >> "$MOCK_LOG"
exit 0
MOCK
cat > "$tmp/bin/mysql" <<'MOCK'
#!/usr/bin/env bash
exit 0
MOCK
cat > "$tmp/bin/mysqldump" <<'MOCK'
#!/usr/bin/env bash
if [[ "${MOCK_DUMP_FAIL:-0}" == 1 ]]; then exit 12; fi
printf 'CREATE DATABASE IF NOT EXISTS inventory;\n'
MOCK
cat > "$tmp/bin/age" <<'MOCK'
#!/usr/bin/env bash
if [[ "$1" == "-d" ]]; then
  # Only test fixture: do NOT interpret this as real encryption.
  cat "${@: -1}"
  exit
fi
if [[ "$1" != "-r" || -z "$2" ]]; then exit 2; fi
if [[ "${MOCK_AGE_FAIL:-0}" == 1 ]]; then exit 88; fi
if [[ "${3:-}" == "-o" ]]; then cat > "$4"; else cat; fi
MOCK
chmod +x "$tmp/bin/"*
export MOCK_LOG="$tmp/systemctl.log"
export PATH="$tmp/bin:$PATH"
cat > "$tmp/backup.conf" <<CFG
BACKUP_DIR="$tmp/backups"
PHOTO_DIR="$tmp/photos"
MYSQL_DEFAULTS_FILE="$tmp/mysql.cnf"
MYSQL_DATABASE="inventory"
AGE_RECIPIENT="age1fixture"
RETAIN_DAYS=14
CONFIG_FILES=("$tmp/local.conf")
CFG
chmod 600 "$tmp/backup.conf"
bash "$repo/scripts/Backup-Ubuntu.sh" --check "$tmp/backup.conf"
bash "$repo/scripts/Backup-Ubuntu.sh" --run "$tmp/backup.conf"
mapfile -t files < <(find "$tmp/backups" -maxdepth 1 -type f -name 'otthontar-*.tar.age')
[[ "${#files[@]}" -eq 1 ]] || { echo "Expected one archive" >&2; exit 1; }
tar -tf "${files[0]}" | grep -q '^photos/image.jpg$'
tar -tf "${files[0]}" | grep -q '^config/file-0$'
printf 'fixture private identity\n' > "$tmp/identity"
bash "$repo/scripts/Verify-Backup-Ubuntu.sh" "${files[0]}" "$tmp/identity"

: > "$MOCK_LOG"
if MOCK_DUMP_FAIL=1 bash "$repo/scripts/Backup-Ubuntu.sh" --run "$tmp/backup.conf"; then
  echo "Failing mysqldump should fail backup" >&2; exit 1
fi
grep -q 'stop otthontar-inventory.service' "$MOCK_LOG"
grep -q 'start otthontar-inventory.service' "$MOCK_LOG"
mapfile -t after < <(find "$tmp/backups" -maxdepth 1 -type f -name 'otthontar-*.tar.age')
[[ "${#after[@]}" -eq 1 ]] || { echo "Failed backup altered completed archives" >&2; exit 1; }

if bash "$repo/scripts/Backup-Ubuntu.sh" --check "$tmp/does-not-exist"; then
  echo "Missing config should fail" >&2; exit 1
fi

: > "$MOCK_LOG"
if MOCK_AGE_FAIL=1 bash "$repo/scripts/Backup-Ubuntu.sh" --run "$tmp/backup.conf"; then
  echo "Failed encryption should fail backup" >&2; exit 1
fi
grep -q 'start otthontar-inventory.service' "$MOCK_LOG"
mapfile -t after < <(find "$tmp/backups" -maxdepth 1 -type f -name 'otthontar-*.tar.age')
[[ "${#after[@]}" -eq 1 ]] || { echo "Failed encryption altered archive count" >&2; exit 1; }

exec 7>"$tmp/backups/.backup.lock"
flock -n 7
if bash "$repo/scripts/Backup-Ubuntu.sh" --run "$tmp/backup.conf"; then
  echo "Concurrent backup should be rejected" >&2; exit 1
fi
exec 7>&-

printf 'not-an-archive' > "$tmp/corrupt.tar.age"
if bash "$repo/scripts/Verify-Backup-Ubuntu.sh" "$tmp/corrupt.tar.age" "$tmp/identity"; then
  echo "Corrupt archive should be rejected" >&2; exit 1
fi
printf 'Fixture tests OK: snapshot, checksum, dump/encryption failure, lock, corruption, config validation.\\n'
