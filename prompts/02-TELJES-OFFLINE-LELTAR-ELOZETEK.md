# 02 – Teljes leltár offline, minden fényképből kis előnézet

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** Előfeltétel a [01C webes felület](01C-WEB-FELULET.md), a működő JPA-alap és a védett iPhone-os elérés a Cloudflare-es címen.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-24. [Státuszszabályok](../projekt/STATUSZOK.md).


Olvasd el a [közös szabályokat](00-KOZOS-UTASITASOK.md), a [PWA-alap](01-PWA-HTTPS-MOBIL-ALAP.md) és a [webes felület](01C-WEB-FELULET.md) leírását. Az offline olvasást a 01C-ben kialakított leltárnézetre építsd. Az eszközpróbát a `https://boroscode.com/otthontar/` címről telepített PWA-val végezd.

## Feladat
A telefon tárolja helyben a **teljes leltár metaadatait** (helyszínek, tárgyak, műveletek, státuszok, eladás, lomtár, áthelyezések, képlista, sorrend, borító, verziók; a valós adatmodell szerint) és **minden egyes tárgy minden fényképének kisméretű előnézetét**. Az eredeti teljes méretű képek ne legyenek a tömeges offline letöltés részei. Offline minden tárgy és kis fotója megtekinthető legyen, a képek hiányát és a szinkronizálás készültségét pontosan jelezze.

## Tervezési elv
- IndexedDB és kényelmes, karbantartható adatréteg (pl. Dexie) külön verziózott sémával: `locations`, `items`, `photoMetadata`, `thumbnailBlobs`, `syncState`, későbbi, a 03-as fázisban bevezetendő `outbox` struktúra. Használj stabil UUID/azonosítókat.
- Offline adatforrás elsődlegesen az IndexedDB; ne csak a HTTP-cache-re hagyatkozz. UI renderelés offline adatokból; újratöltés/bezárás után megmarad.
- Online teljes inicializáló másolat: az összes tárgy, helyszín, státusz, minden képelőnézet letöltése naplózott, megszakítható/folytatható, lapozott/batch munkával. Kihagyás nélküli konzisztencia, nagy leltárnál ne fagyjon meg a böngésző. Állapot: 0..100%, utolsó sikeres szinkron, hiányzó képek, becsült tárhely.
- Ha a meglévő `GET /api/items?trash=false` nem elegendő (például nem tartalmazza a lomtárat vagy képlista-adatot), új, visszamenőleg kompatibilis olvasó API-t tervezz, ne törj el meglévő végpontokat. Biztosíts konzisztens snapshotot ÉS hozzá illeszkedő tartós változásnaplót/inkrementális változáslekérési protokollt, hogy inicializálás alatt sem maradjon el változás.
- A meglévő `GET /api/photos/{id}/small` végpontot használd, ha megfelelő; vizsgáld meg, hogy maximum 420 px előnézet elég-e. A cél pl. kis JPEG/WebP előnézet, de ne ronts el korábbi képfájlokat és ne duplikálj feleslegesen képeket. A tartalom/típus és orientáció legyen helyes.
- Képekre és metaadatokra kontrollált concurrency, retry, checksum/integritás (ahol indokolt), quota exceeded és részleges hiba kezelése. Nem sikerült képekkel NE jelenjen meg „teljes offline kész” állapot.
- Kérj `navigator.storage.persist()` perzisztenciát, ha támogatott, ellenőrizd `navigator.storage.estimate()` segítségével a kvótát; soha ne tekintsd garantáltnak az iOS tartós tárolását. A helyi adatok törlése vagy kvóta problémája legyen egyértelműen jelezve. Későbbi offline írásokhoz maradjon hely.

## Teljes méretű képek megnyitáskor
- Tárgy megnyitásakor/szerkesztésekor azonnal mutasd az IndexedDB-ben lévő kis képet.
- Ha a backend valóban elérhető, az éppen megtekintett tárgy képeinek teljes méretű változatát kérd le **igény szerint**, pl. adott kép megnyitásakor (ha a teljes galéria megnyitása ésszerű, a letöltés legyen korlátos). A kliens kis képről váltson nagy felbontásra a betöltés után.
- Ha a szerver nem elérhető, maradjon a kis előnézet. A teljes méretű kép hiánya ne akadályozza az adatlap szerkesztését.
- A nagy képek tárolására legfeljebb méretkorlátos, üríthető LRU-cache-t használj. **Nem szabad** azonos cache-szabályokkal kezelni a feltöltésre váró új képek eredetijeit (azokat tilos automatikusan kidobni).

## Elfogadási feltételek
- Egy online teljes szinkron és offline módra váltás után az **összes tárgy, helyszín, releváns állapot és minden kis előnézet** elérhető a telefonon.
- 100, 1000 és lehetőség szerint 5000 tételes szimulált leltárnál sincs memóriakifutás, UI-fagyás vagy csendes hiány.
- Offline újraindítás után is van leltár; teljes képet nem próbál offline lekérni és nem keveri össze az előnézettel.
- Online adott fotó megtekintésekor a teljes felbontás dinamikusan betöltődik, nem indul tömeges eredetikép-letöltés.
- A releváns automatikus tesztek lefutnak; manuális iPhone tesztlista elkészül.

## Konzisztens snapshot és előnézetek – kötelező tervezési ellenőrzések
- A jelenlegi `V1__inventory.sql` szerint a `photo` külön táblában van; `item.cover` képre mutat, a `V2` hozzáadja az `item_action`-t és a célhelyszínt. A jelenlegi `GET /api/items` nem feltétlen adja vissza az összes képet; ellenőrizd az API-kontraktust és szükség esetén olvasó snapshot-végpontot adj hozzá (tárgy + teljes képlista + borító + sorrend + azonosító/verzió + tárgyművelet + lomtár + helyszín).
- A **snapshot konzisztenciahatárát** definiáld: szerveroldali cursor/watermark és lapozás, változásnaplóval összekapcsolva, hogy a közben módosult/törölt rekordok se vesszenek el. A kliens csak minden rekord és előnézet sikeres integritásellenőrzése után mozdítsa el a „teljes offline kész” állapotot/cursort; részleges letöltésből tudjon folytatni.
- Az előnézeti képeket fotóazonosító + változat/ellenőrzőösszeg szerint tárold, különben egy frissített fotóhoz régi előnézet maradhat. Ha az aktuális szerver nem változtat meg képet azonos ID mellett, azt ellenőrizd és dokumentáld. A törölt képek helyi bélyegképeit takarítsd biztonságosan, de a pending offline fotókat soha.
- iOS WebKit quota/eviction: a kvóta nem fix GB és `navigator.storage.persist()` nem ígéret; becslés alapján valós kapacitásfigyelés és kvótahiba-UI kell. **Helyi cache törlése / iOS tárhelytisztítás után** újabb teljes inicializálás legyen lehetséges, a fel nem töltött outbox biztonságos mentésével. Ne használj agresszív cache evictiont a komplett offline leltár bélyegképein.
- Teszteld az „egy tárgyban több kép”, „lomtárban lévő tárgy”, „PC-n törölt/áthelyezett/fotóval bővített tárgy” eseteket; ezek az offline másolatban helyesen jelenjenek meg. A borítókép/galéria minden képe offline megjeleníthető legyen kicsiben.

## Fázishatár pontosítása: olvasási napló alapja a 02-es fázisban
- A korábbi „snapshot **vagy** változáslekérési protokoll” megfogalmazás **nem elegendő**: ennek a fázisnak konzisztens snapshotot **és** hozzá illeszkedő, tartós szerveroldali változásnaplót, monoton cursort és tombstone-okat kell kiépítenie, különben a 03-as offline írási fázisnak nem lenne stabil alapja. A változásnapló bevezetése az összes meglévő desktop írási útvonalra (pl. `Ledger.save`, `place`, `completeSale`, `transition`, `Actions.save`) már **itt történjen meg**; a 03-ban csak bővítés/továbbhasználat legyen, ne második napló.
- Előfeltétel: az adatbázis meglévő `V1__inventory.sql` és `V2__item_actions.sql` migrációi változatlanok; első új sémaváltozás az aktuális állapot alapján következő Flyway verzió. Idempotens írási táblák csak a 03-as fázisban, külön új migrációval.
- A jelenlegi `Ledger.items(boolean trash)` **csak** az item + location összekapcsolt listáját adja; a teljes fotólistát `Ledger.item(UUID id)` adja, ahol a `photo` rekordban jelenleg `id` és `filename` érkezik, adatbázisban pedig `position_no` szerepel. Emiatt a konzisztens, lapozott snapshothoz célzott új read API indokolt; egyszerű kliensoldali N+1 lekérés nem stabil snapshot. Az `Actions.all()` tartalmát is foglald bele. Nézd át a meglevő `/api/items?trash=true` lomtárat.
- A felhasználó szerkeszthetőséget csak a 03-as fázis után kap. A 02-es fázisban **csak olvasható** offline felület, offline teljes előnézet-galéria és online, konkrét képmegnyitáskor eredetikép-lekérés készül.
- Minden sikeres snapshot átadásához tesztelj egy párhuzamos PC-módosítást és végleges törlést; a cursor rögzítése és a commit-sorrend nem hagyhat el látható változást. Az utolsó teljesen ellenőrzött snapshot offline elérhető maradjon egy megszakadt frissítés után.
