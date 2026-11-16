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
