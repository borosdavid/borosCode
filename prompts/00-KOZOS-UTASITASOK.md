# Otthontár iPhone offline-first PWA – közös megvalósítási szabályok

**Repo:** `borosdavid/borosCode`, fejlesztési ág: `dev`; promptok és roadmapek hiteles forrása: `main` (lásd [AGENTS.md](../AGENTS.md)).
**Cél:** az iPhone 12 Pro készüléken, Mac és App Store nélkül telepíthető, Safari kezdőképernyős Otthontár PWA; a TELJES tárgy- és helyszínleltár offline, offline minden tárgyhoz KIS ELŐNÉZETI kép(ek), a teljes méretű képek csak akkor töltődjenek le, amikor a felhasználó megnyitja/szerkeszti a tárgyat ÉS elérhető a szerver. Az offline készített ÚJ fotók eredeti változata feltöltésig helyben marad.

**A repository tényleges kódját mindig vizsgáld meg, ne tekintsd készpénznek a prompt feltételezéseit.** A jelenlegi README szerint: Java/Spring Boot, Thymeleaf, MySQL/Flyway, imaging szolgáltatás, `GET /api/places`, `GET /api/items?trash=false`, `GET /api/items/{id}`, `POST /api/items` multipart, `GET /api/photos/{id}/{original,small}`; `Ledger.Draft` és verzióellenőrzés. A helyi szerver 127.0.0.1:8740-en figyelhet; ez iPhone-ról önmagában nem érhető el. Ellenőrizd a valós API-szerződéseket, verziókat, képtárolási és biztonsági megoldásokat.

## Megkötések minden fázisra
- A meglévő Windows/Thymeleaf munkafolyamatokat és adatokat őrizd meg; a személyes `.local/` profilt, adatbázist, fényképeket és Docker-volumeket soha ne töröld.
- Minden sémaváltozás kizárólag új Flyway migrációban; már alkalmazott V1/V2 migrációt ne írd át.
- **Ne tegyél közzé adatbázist, Java belső szolgáltatást, képtárolót vagy hitelesítés nélküli REST API-t a nyilvános internetre.** A távoli és Wi-Fi-s kliensnek biztonságos HTTPS origin és megfelelő hozzáférés-kontroll kell. Ne égesd be titkokat a PWA-ba vagy repóba.
- A hálózattól független offline működés nem azonos azzal, hogy a telefon soha nem járt a szerveren: első online aktiválás/szinkron és PWA telepítés kell.
- iOS-en nem támaszkodhatsz megbízható háttérszinkronra, háttérben futásra vagy böngészőtárhely-örökéletre. Alkalmazásmegnyitáskor, foregroundban, explicit gombbal szinkronizálj; kérj tárhely-perzisztenciát ahol támogatott, kezeld a kvótát és készíts export/mentési megoldást a még fel nem töltött adatokhoz.
- Az offline leltár **minden rekordját** és az összes hozzá tartozó előnézeti képét szinkronizáld; az eredeti nagy képeket NE töltsd le tömegesen; legfeljebb felhasználó által megtekintett képek kerülhetnek opcionális, limitált/cache-elt tárolásba. Hiányzó kis kép esetén jelölj külön hibát, ne mutasd tévesen „kész” offline állapotúnak.
- Az offline készített fényképek nyers/eredeti feltöltendő példányát tartsd meg a sikeres, szerver által visszaigazolt mentésig; tárolj belőlük a leltárhoz kis előnézetet is. Tájékoztasd a felhasználót a tárhelyhasználatról és a nem szinkronizált tételekről.
- Feltöltés és újrapróbálkozás legyen idempotens (stabil műveletazonosító), verzióütközés esetén ne legyen néma felülírás. A törlés/lomtár/áthelyezés/borítókép-sorrend változásait is konzisztensen kezeld.
- Akadály vagy feloldatlan termékdöntés esetén írd le és kérdezz, mielőtt destruktív vagy biztonságilag jelentős módosítást végzel.
- Minden fázis végén: megváltoztatott fájlok, teszteredmények, kézi iOS tesztlista, ismert kockázatok, következő fázis előfeltételei. Ne állítsd, hogy valódi iPhone-on teszteltél, ha nem történt meg.

## Hibernate/JPA előfeltétel
Az újonnan hozzáadott `prompts/00A-HIBERNATE-JPA-ATALLAS.md` a 01–04-es iOS/PWA fázisok **előtt** futtatandó, külön perzisztencia-refaktor. Ha már megtörtént, ellenőrizd a Hibernate/JPA végállapotot és a teszteket, ne végezd el újra. A 01–04-es promptok régi `JdbcTemplate` / `Ledger` / `Photos` hivatkozásai **kiinduló történeti állapotot** írnak le: a Codex a 00A utáni tényleges JPA entitás-, repository- és service-rétegből induljon, ne állítsa vissza a JDBC-t. A meglévő Flyway migrációk továbbra sem módosíthatók.

## Ajánlott futtatás
A feladatok sorrendjét a [promptindex](README.md) rögzíti. A működő webes alapra épülő 01C felület elkészítése megelőzi a 02–04 iPhone-os offline fejlesztést. Egy fázis után ellenőrizd a változásokat, teszteld, és csak azután indítsd a következőt. A promptok **nem végrehajtandók automatikusan** attól, hogy egy mappában léteznek.

## Hozzáférés és architektúra
- **Kanonikus alkalmazáscím:** a böngészős felület és az iPhone-ról telepített PWA címe `https://boroscode.com/otthontar/`. Az Ubuntu-példány webes forgalmát a Cloudflare Worker és Tunnel továbbítja. A háttérhostot szolgáltatási hitelesítés védi, a felhasználó az Otthontár saját fiókjával jelentkezik be. A manifest, a Service Worker, az alkalmazáslinkek, az API-, kép- és statikus erőforrások útvonalai illeszkedjenek az `/otthontar/` telepítési előtaghoz. A PWA hatóköre maradjon az Otthontár útvonalán.
- **Privát üzemeltetés:** iPhone-ról az iSH-ban indított SSH-kapcsolat a Tailscale privát hálózatán éri el a szervert. Az alkalmazás használatához a Cloudflare-es címen internet és alkalmazásfiók kell. A helyi cím és a Tailscale HTTPS-cím eltérő böngészős origin, külön Service Workerrel és IndexedDB-vel. Az offline próbát a kanonikus címről előzetesen telepített PWA-val, repülőgép módban végezd. [Távoli hozzáférés](../projekt/SZERVER-TAVOLI-HOZZAFERES.md).
- **Adatvesztés elleni garancia:** az iOS IndexedDB-t az operációs rendszer törölheti; a perzisztencia-kérés nem garancia. A nem szinkronizált módosításokhoz ellenőrizhető export-visszaállítási terv kell, még a széles körű használat előtt. Soha ne jelents 100% kész állapotot a metaadatok és az összes elvárt előnézet ellenőrzése nélkül.
- **Jogosultsági határok:** VPN-hozzáférés önmagában nem feltétlen alkalmazásszintű hitelesítés. Ellenőrizd a többfelhasználós/egyetlen háztartás jogosultsági modellt, és védd a PWA-ból elérhető adatokat, képeket és szinkron végpontokat. Offline helyi adatok érzékenyek, automatikus felhőmentésük ne történjen.
- **Szinkron minden írási útvonalból:** a desktop webes módosítások, tárgy-/fotóváltozások, eladási státusz, műveletek és lomtár is generáljon konzisztenst változásnaplót; a mobil API önmagában történő naplózása hibás.
- **Konkrét viselkedés ütközéskor:** soha ne írja felül a szerver változatát néma last-write-wins; az offline helyi módosítás és eredeti fotó megőrzendő a konfliktus kézi feloldásáig.

## Fázisok közötti szerződés és indulási feltételek (auditált)
- A 01-es fázis PWA/HTTPS és **biztonságos elérés**; a távoli használatot hitelesítés/jogosultság nélkül **nem** szabad engedélyezni. Ha nincs elfogadható védelem, a szolgáltatás maradjon helyileg elérhető, a mobilteszt blokkolt; a 04-es fázisban csak auditálni és erősíteni kell az 01-ben kialakított védelmet.
- A 02-es fázis feladata a **read-side konzisztens teljes snapshot és a változásnapló/inkrementális olvasási alap** kialakítása is (ideértve a korábban létező desktop írási útvonalak naplózását). A 03-as fázis erre építve készíti az offline mutációkat, outboxot, szerveroldali idempotens író/sync API-kat és konfliktusfeloldást. Ne legyen két, egymással versengő változásnapló.
- Az offline adatvesztés megelőzése nem halasztható a 04-es fázisig: a 03-as fázis **kész feltétele** az outbox + fotóblobok felhasználó által indítható, ténylegesen visszaállítható mentése/importja, legalább minimálisan; a 04-es fázis ezt terhelés- és iPhone-próbával megerősíti.
- A 02-es fázis végéig a telefon leltára **csak olvasható offline**; a 03-as fázistól lép életbe a helyi szerkesztés. A 02-es fázisban ne ígérjen offline szerkesztést vagy szinkronizált feltöltést a felület.
- A kétirányú szinkronizáció közös modellje az összes meglévő írási útvonalat lefedi (tárgyak, helyszínek, egyedi műveletek, képek, eladás, lomtár/purge); a 02-es és 03-as fázis ugyanazt a cursor/verzió/tombstone szerződést használja.
- A **teljes offline leltár** minden tárgy metaadatait és minden kép kis előnézetét jelenti, nem minden eredeti kép offline tárolását. A szerveren tárolt eredeti csak az adott kép megtekintésekor, online kérhető le; a nem szinkronizált új fotók feltöltendő eredeti példánya kivétel, offline kötelezően megőrzendő.

## Elfogadott alkalmazásjogosultság – 2026-11-17

D-008 alapján minden felhasználó felhasználónév/jelszó segítségével jelentkezik be; minden aktív bejelentkezett fiók azonos teljes alkalmazás-hozzáférést kap a közös adatokhoz. A részletes szerződés: [01A fiókok](01A-FELHASZNALOI-FIOKOK.md). Külön alkalmazásadmin-szerepet, külön felhasználói leltárat vagy VPN-only hitelesítést ne vezess be ettől eltérően. Az alkalmazásjog nem ad SSH/Linux/GitHub-jogot. A későbbi offline adatkezeléskor a kijelentkezést/fiókváltást és a nem szinkronizált adatok megőrzését is kövesd.
