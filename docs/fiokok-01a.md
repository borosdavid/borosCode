# 01A – Felhasználói fiókok és bejelentkezés

Forrás: `main` prompt-állapot `302b56fb1f0255eae89c84247772b94b3f699748`, `prompts/01A-FELHASZNALOI-FIOKOK.md`. Kiinduló kód: JPA/PWA `dev` `dae1224627a983217296ffe2dd47a339caacdb59`. Dátum: 2026-11-17.

## Elkészült működés

- Spring Security, szerveroldali session, felhasználónév/jelszó, be- és kijelentkezés. Minden aktív fiók azonos `ROLE_USER` joggal ugyanazt a közös leltárt éri el. Nem készült külön alkalmazásadmin-szerepkör vagy felhasználónkénti adatleválasztás.
- Új, önálló `V3__application_accounts.sql` migráció és JPA-fióktárolás. A V1/V2, a meglévő leltártáblák és a JPEG-ek megmaradnak.
- A felhasználónév kisbetűre normalizált, környező szóköz nélkül; 3–64 ASCII betű/szám/pont/kötőjel/aláhúzás, első karakter betű vagy szám. A belépés ezért kis-/nagybetűtől független.
- Sózott `{bcrypt}` jelszóhash, költség 12; karbantartáskor legalább 12 karakter, legfeljebb 72 UTF-8 bájt. Nincs alapfiók, generált alapjelszó vagy publikus regisztráció.
- A session azonosítója belépéskor változik, 30 perc inaktivitás után lejár. A cookie HttpOnly/SameSite=Lax; HTTPS-módban kötelező Secure. Letiltás vagy jelszócsere után a régi session következő online kérésénél az adatbázisállapot ellenőrzése érvényteleníti a munkamenetet. Újraindítás után is új belépés kell.
- Valódi session-CSRF védi a login/logout, JSON és multipart írásokat is. A régi `X-Otthontar-Request: 1` fejléc megmarad, de nem helyettesíti a belépést/CSRF-et. Érzékeny API-k, kis és eredeti képek, leltároldalak bejelentkezés nélkül nem érhetők el.
- A login és a személyes adat nélküli PWA-erőforrások elérhetők belépés nélkül. A minimális `/actuator/health` loopbacken megmarad az indítók számára; a konfigurált külső HTTPS-hoston tiltott. A health nem ad belépést vagy leltáradatot.
- API-hibánál 401 JSON-válasz érkezik HTML-loginoldal helyett. A nyitott szerkesztő új belépési dialógust mutat, a mezőket és fotókat megőrzi; siker után a felhasználó kézzel újra megnyomja a Mentés gombot. Nincs automatikus POST-újrapróbálás.
- Az írás a session-CSRF mellett `X-Otthontar-User` fiókazonosítót is kér. A szerkesztő előzetesen ellenőrzi a fiókot, és a szerver is összeveti az azonosítót; egy másik tabon történt fiókváltás nem tölti fel csendben az előző felhasználó piszkozatát.
- A próbálkozáskorlát folyamaton belüli, 15 perces ablak: fiókonként 5 egymást követő sikertelen próbálkozás, socket-peer címenként legfeljebb 20 próbálkozás (a sikerek is számítanak). Siker esetén a fiókszámláló törlődik. A tároló legfeljebb 4096 aktív kulcs, beteléskor elutasít. A loopback proxy mögötti felhasználók peer-limitje közös; újraindítás nullázza a korlátot. A konkrét hardveren a bcrypt költsége és a háztartási terhelés még mérendő.

## Első fiók és karbantartás

**A frissített alkalmazás minden leltárhasználathoz belépést kér; első fiókot helyben kell létrehozni.** Ehhez az adatbázisnak futnia kell, a megfelelő DB-környezeti változókkal és frissen buildelt JAR-ral. A karbantartó külön, webkiszolgáló és képtakarító ütemezés nélküli adatbázis-kontextust indít, majd bezárja. Nem indítja vagy állítja le a futó alkalmazást.

Jelszót csak interaktív terminálban kér, kétszer, visszhang nélkül. Nem adható meg argumentumként vagy alkalmazásjelszó-környezeti változóként. A DB csatlakozási titok a meglévő helyi konfigurációból származik; azt ne másold Gitbe vagy chatbe.

Windows natív profil, a repó gyökeréből, a meglévő helyi profiladatokkal:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/Account.ps1 -Action create -UserName david
powershell -ExecutionPolicy Bypass -File scripts/Account.ps1 -Action password -UserName david
powershell -ExecutionPolicy Bypass -File scripts/Account.ps1 -Action disable -UserName david
powershell -ExecutionPolicy Bypass -File scripts/Account.ps1 -Action enable -UserName david
```

Az izolált Windows tesztprofilhoz ugyanezek `-TestRun` kapcsolóval használhatók. A helper csak a kiválasztott profil `access.json` DB-adatait olvassa, és az ideiglenes környezeti változókat visszaállítja. Nem hoz létre új DB-profilt. A helper Windows alatt itt nem volt futtatható.

Általános Linux/Java indítás, már beállított DB-környezettel:

```bash
java -jar inventory/target/inventory-1.0.0.jar account create david
java -jar inventory/target/inventory-1.0.0.jar account password david
java -jar inventory/target/inventory-1.0.0.jar account disable david
java -jar inventory/target/inventory-1.0.0.jar account enable david
```

Dockerben, már futó adatbázis és friss image esetén:

```bash
docker compose run --rm --no-deps -it inventory account create david
```

A név a példában fióknév, nincs hozzá előre meghatározott jelszó. Fióktörlés helyett letiltás javasolt; a leltár ettől közös és megmarad. A fióktábla és a jelszóhashek az adatbázismentés részei.

## HTTP és HTTPS konfiguráció

- Alapból a távoli host/proxy továbbra is tiltott. A natív app loopbacken marad; a `PUBLIC_ORIGIN` üres.
- A Secure cookie alapértéke true. A Windows helyi HTTP-indító csak akkor állít `SESSION_COOKIE_SECURE=false` értéket, ha nincs public origin és nincs explicit cookiebeállítás. A mellékelt Compose-profil szintén csak a gazdagép loopbackére publikált HTTP, ehhez továbbítja a `SESSION_COOKIE_SECURE` értéket, alapból false-t.
- Natív, loopbacken futó app elé tett valódi HTTPS-proxyhoz `PUBLIC_ORIGIN=https://<kiosztott-host>.ts.net`, `SESSION_COOKIE_SECURE=true` és loopback `BIND_ADDRESS` kell. Más séma, port, útvonal, userinfo és nem loopback bind esetén a public-origin konfiguráció induláskor elutasított.
- Csak az adott host és tényleges loopback socket-peer kap proxyengedélyt. Standard `Forwarded` tiltott, az opcionális forwarded host/proto nem mondhat ellent a konfigurációnak. A Spring általános forwarded-header értelmezése továbbra is kikapcsolt; a már ellenőrzött kérés kap rögzített HTTPS-sémát. A peer-rate-limit nem bízik kliens által állított IP-fejlécben.
- Ha a loopback proxy külön háttérhostot küld az `X-Forwarded-Host` fejlécben, a `PROXY_FORWARDED_HOST` értéke pontosan ez a hostnév (Cloudflare Tunnel esetén `otthontar-origin.boroscode.com`). A `Host` fejlécnek továbbra is a `PUBLIC_ORIGIN` hostnevével kell egyeznie (`boroscode.com`).
- A Tailscale eszközjogosultságait és a proxy valódi fejléceit még az adott gépen kell ellenőrizni. A Docker mögötti távoli TLS/proxy topológia **nem része ennek az alap Compose-profilnak**; a natív proxyengedélyt ne kerüld meg hostátírással vagy a védelem kikapcsolásával. Debian Docker-telepítéshez külön átadási próba kell.

## Közvetlen API-kliensek és tesztek

1. GET `/auth/csrf`, a session cookie és a válaszban kapott CSRF-token megtartása.
2. POST `/login` form-urlencoded `username`/`password`, a CSRF a kapott fejlécben; `Accept: application/json` esetén siker: 204. A session cookie frissül.
3. GET `/api/session`: stabil `id`, `username`, `headerName`, `token`. Írásokhoz a friss CSRF-fejléc, `X-Otthontar-Request: 1` és `X-Otthontar-User: <id>` szükséges.
4. GET/POST során a cookie megőrzendő. 401-nél új belépés, automatikus írókérés-újraküldés nélkül.

A Windows `tests/acceptance.cjs` most valódi tesztfiókkal lép be, és a böngésző cookie-jait használja a közvetlen API-kérésekhez is. Külön izolált fiókot kell előre provisionálni, és a tesztfolyamatnak `OTTHONTAR_TEST_USERNAME`/`OTTHONTAR_TEST_PASSWORD` értéket adni. Ez tesztkliens-beállítás, nem alkalmazásba égetett fiók. A jelszót ne add ki naplóban vagy commitban. A `PersistenceIT` saját, futáskor generált tesztjelszót/fiókot használ, belépéssel és CSRF-fel; nincs tesztprofilos hitelesítéskikapcsolás.

## Ellenőrzés – ebben a környezetben

- Teljes JDK 21, Maven 3.9.11: `mvn spotless:apply verify` **BUILD SUCCESS**, **18 Java-teszt**, 0 hiba/0 kihagyás. Ebből 6 Spring Security/MockMvc fiókteszt és 5 host/proxy-határteszt; további 3 leltár- és 4 képteszt. A security próbák mockolt fiókrepositoryval futnak, nem valódi MySQL-en.
- `node tests/pwa.cjs`: **4 sikeres böngészős csoport** Chromium Headless Shell 131 alatt: korábbi PWA/fotó/draft tesztek, plusz 401/újrabelépés/adatmegőrzés és fiókváltási védelem. A fixture szimulálja a session-válaszokat; az igazi Spring filterláncot a Java-tesztek ellenőrzik.
- JavaScript szintaxis és `git diff --check`: sikeres. A Mockito teszt-double interfész-proxyt használ; a rendszer Chromiumának korlátozott indíthatósága miatt külön headless böngészővel futott a próba.
- **Nem futott ebben a körben:** valódi MySQL V3-migráció és fiók-karbantartás, a módosított `mysql-it` integrációs profil, Windows PowerShell/acceptance, Docker és tényleges HTTPS/Tailscale/iPhone. A kód és a tesztharness lefordult, de a felsorolt működéspróbák hátravan.

## Hátralévő átadási feltételek és offline szerződés

- Izolált valódi MySQL-en V3-migráció, két fiók létrehozása, jelszócsere/letiltás és újrafuttatott integrációs/Windowsos próbák.
- Valódi HTTPS-proxyval és iPhone-on be-/kijelentkezés, Secure cookie, fotómentés és sessionlejárat. A korábban jelzett Windows/Edge PWA offline deep-link hibát itt nem nyilvánítjuk megoldottnak; a Headless 131 teszt sikeres.
- A 02/03 fázisban a helyi leltár/cache felhasználóazonosítóhoz kötendő, kijelentkezés/fiókváltáskor másik fióknak nem hozzáférhető. Nem szinkronizált outbox/fotó nem törölhető csendben vagy tölthető fel másik fiók nevében; megőrzés/export/felhasználói döntés kell. Távoli letiltás offline kliensen azonnal nem garantálható. Ebben a fázisban még nincs teljes offline leltár vagy outbox.

Ez `dev`-mentés, **nem tesztágas átadás és nem kész élő szerverkapcsolat**. A 01A prompt aktív marad a mainben a kötelező elfogadási próbákig; kiadási taget nem készítünk.
