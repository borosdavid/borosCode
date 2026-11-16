# Otthontár iPhone offline-first PWA – közös megvalósítási szabályok

**Repo:** `borosdavid/borosCode`, alapág: `main`.
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

## Ajánlott futtatás
A fájlokat a sorrendjükben add át Codexnek. Egy fázis után ellenőrizd a változásokat, teszteld, és csak azután indítsd a következőt. A promptok **nem végrehajtandók automatikusan** attól, hogy egy mappában léteznek.

## Kötelező architekturális döntések (pontosítás, 2026-11-16)
- **Egységes HTTPS origin:** a telepített PWA végig ugyanazon `https://<host>.ts.net` eredetről használja a felületet és az API-t. A `127.0.0.1`, LAN-IP vagy másik HTTPS host nem felcserélhető: külön origin, külön IndexedDB/Service Worker; ne vezess be csendes hostváltást, ami „eltünteti” a leltárt. A Tailscale Serve a localhoston maradó 8740-es webszolgáltatás elé tehető, Funnel nélkül; a beállítás kézi, felhasználói jóváhagyást igényel. Az iPhone Tailscale-kapcsolata csak az online szakaszhoz kell; a korábban feltöltött PWA offline VPN nélkül is nyíljon meg.
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
