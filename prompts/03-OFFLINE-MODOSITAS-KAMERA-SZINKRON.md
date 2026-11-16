# 03 – Offline szerkesztés, iPhone kamera, biztonságos kétirányú szinkronizáció

Olvasd el a `prompts/00-KOZOS-UTASITASOK.md`, `01-PWA-HTTPS-MOBIL-ALAP.md` és `02-TELJES-OFFLINE-LELTAR-ELOZETEK.md` fájlokat. A valós szerveroldali `Ledger.Draft`, fotókezelés, tranzakciók, verziószámok és migrációk ellenőrzése kötelező.

## Cél
A mobil PWA internet nélkül is létrehozhasson, módosíthasson, lomtárba helyezhessen és költöztethessen tárgyakat, helyszíneket és a jelenlegi adatmodell által támogatott mezőket. Új tárgyhoz iPhone kamerával készített több fotó is csatolható, borító állítható. Visszatérő hálózati kapcsolatkor az összes módosítás biztonságosan felkerül a központi MySQL-adatbázisba, és a többi kliens változásai is visszatöltődnek.

## Offline módosítások
1. IndexedDB írási tranzakciók és tartós outbox: egyedi műveletazonosító, eszközazonosító, entitásazonosító, alapverzió, módosított mezők/adatok, függőségek, állapot, próbálkozások. Új rekordok helyi UUID-val létrehozhatók. Konzekvens függőségi sorrend (helyszín → tárgy → fotó).
2. Offline fényképezés: iPhone kamera/fotótár választás, több JPEG (vagy biztonságos HEIC-konverzió), megfelelő EXIF-orientáció, borító/sorrend/eltávolítás kezelése. A **feltöltendő eredeti minőségű JPEG** maradjon helyben és az **összes képből külön előnézet** is legyen. A költséges eredeti JPEG-et CSAK szerveroldali sikeres, ellenőrzött elmentés és visszaigazolás után lehet helyileg felszabadítani (a kis kép megmarad).
3. Tárgyakat, fotókat vagy várakozó módosításokat SOHA ne törölj csupán azért, mert a képfeltöltés időtúllépésre futott, lezárult a PWA, újraindult a telefon vagy elfogyott a tárhely. Jelölj „mentésre vár”, „szinkronizálás alatt”, „ütközés”, „hiba” állapotokat.
4. Helyi törlés/lomtár/visszaállítás/áthelyezés és egyedi műveletek: a jelenlegi üzleti szabályokat tükrözd, de a központi szerver maradjon az igazság forrása szinkron után.

## Szerver és szinkron API
5. Vizsgáld meg, hogy a meglévő `POST /api/items` multipart és verzióellenőrzés biztonságosan támogatja-e az offline műveleti sort. Szükség esetén új, visszamenőleg kompatibilis szinkron API-kat adj hozzá, amelyek kezelik az idempotenciát, változásnaplót/tombstone-okat, képek SHA/azonosító ellenőrzését és a felhasználói jogosultságot. Ne alakítsd át kockázatosan a meglévő asztali végpontokat.
6. Változások lehúzása szerveroldali monoton cursorról/naplóról, nem pusztán helyi órák összehasonlításából. Első teljes snapshot + későbbi inkrementális változásletöltés, törölt rekordok és fotók feldolgozása. A szerver kezelje a lejárt cursor vagy helyi adatok elvesztése miatti teljes újraszinkron lehetőségét, **a helyi fel nem töltött adatok megőrzésével**.
7. Feltöltés retry/backoff, kapcsolatellenőrzés API health/auth szerint, egyértelmű visszaigazolás, explicit „Szinkronizálás” gomb, előtérbe kerüléskor automatikus kísérlet, de NE építs iOS háttérszinkron garanciájára. Batch/fotófeltöltés legyen megszakítható és folytatható, ne jelenjen meg téves siker.
8. Ütközések: ha ugyanazt a rekordot két kliens módosította, 409-re és szerververzióra épülő, értelmes konfliktusfelület kell: „telefonon lévő változat”, „szerver változata”, kézi választás/összefésülés, képeknél biztonságos kezelés. Néma last-write-wins TILOS. Konzisztens borítókép, fotósorrend, mozgás és lomtár.
9. Több eszköz és fotó ugyanannál a tárgynál, megismételt kérés, hálózat megszakadása, szerver újraindítása, adatbázis tranzakció visszagörgetése, kimerült mobil tárhely – mindhez automatikus teszt vagy dokumentált manuális forgatókönyv.

## Elfogadási feltételek
- Repülőgép módban 10 új tárgy és 20 új fotó menthető, az app újraindítása után is elérhetők, a nagy feltöltendő JPEG-ek helyben megvannak.
- Visszakapcsolás után csak visszaigazolt, idempotens szervermentés esetén lesz „szinkronizált” státusz; duplikált tárgy/fotó nem keletkezik.
- Egyidejű módosítás esetén a konfliktus felismerhető és megoldható, adatvesztés nélkül.
- Miután a képek felkerültek, az összes kis előnézet offline megmarad; nagy képek online igény szerint tölthetők le.
- A meglévő desktop leltározás továbbra is működik; migráció és meglévő adatok megmaradnak.

## Tranzakció, képfeltöltés, idempotencia – konkrét követelmények
- A jelenlegi `Ledger.save` szerveroldalon verzióellenőrzést használ és képeket fájlba ír, a `Photos.store` az imaging szolgáltatást is hívja. **A DB-tranzakció és a fájlrendszer nem egyetlen atomi tranzakció**: vizsgáld meg a részleges siker/orphan fájl/sikertelen válasz eseteit. A retry nem hozhat létre dupla tárgyat vagy képet.
- Stabil `operationId` / idempotency key a szerveren **tartósan tárolt eredménnyel**, nem csak memóriában. Ugyanazon kulcs azonos payloadra ugyanazt a logikai választ adja, eltérő payloadra konfliktust; folyamatban lévő művelet és szerver-újraindulás kezelése szükséges. Adatbázis-migráció külön új `V3+` fájlban.
- A változásnapló ugyanabban a DB-tranzakcióban keletkezzen, mint a szerveroldali módosítás, és **minden írási útvonal** (desktop API, eladási műveletek, tárgymozgatás, lomtár/purge, helyszín, action kezelés, fotó) részt vegyen benne. A teljes törléseket tombstone képviselje, stabil monoton sorszámmal/cursorral; kerülni kell a commit-sorrend/cursor versenyhelyzeteket. Teszt: desktop ír szinkron közben.
- A kliens feltöltés előtt tartsák meg a létrehozott tartalom teljes helyi példányát, a letöltött szerveradatokkal való merge nem dobhatja el a függő műveleteket. Idempotens outbox dequeue csak a szerver visszaigazolása után; képfeltöltéskor megszakadt válasz külön próbálkozással is ugyanarra az eredményre jusson.
- A `POST /api/items` várhatóan együtt menti a draftot és a fotókat: ha a nagy képcsomag mobilon megszakad, tervezz kontrollált, visszaigazolható feltöltési/protokoll-lépéseket, tételazonosító + képazonosító megőrzésével; ha szükséges új, backward compatible upload API, de ne változzon a desktop workflow.
- Háttérben/lezárt PWA-n nincs garantált feltöltés: `online` JS esemény önmagában nem elég; mindig szerver-elérhetőséget és jogosultságot ellenőrizz, a foregroundban felhasználói visszajelzéssel szinkronizálj.
- Ütközésfeloldáskor az új fotók, képsorrend, borítókép és tárgyművelet **együttes** érvényesítését vizsgáld meg; kétoldali szerkesztés esetén ne veszíts el fotót. Készíts tesztet helyi + desktop egyidejű módosításra és eladási állapot változtatására is.
