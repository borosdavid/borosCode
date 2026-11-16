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
