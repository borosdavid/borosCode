# 04 – iPhone használhatóság, adatintegritás, offline mentés és átadás

Olvasd el a `prompts/00-KOZOS-UTASITASOK.md` és az 01–03 feladatokat. Ez záró, tesztelési és megerősítési fázis: ne írj újra működő modult indokolatlanul.

## Feladat
Tedd megbízhatóan használhatóvá a teljes offline Otthontár PWA-t iPhone 12 Pro-n; hangsúly: adatvesztés elkerülése, tárolókapacitás, szinkronkonfliktusok, fényképek minősége, hozzáférés és részletes telepítési/tesztelési dokumentáció.

## Részfeladatok
1. Futtass egység-, integrációs- és böngészős teszteket a projekt lehetőségei szerint. Teszteld a teljes snapshotot, minden thumbnail offline hozzáférését, ismételt betöltést, verziókonfliktust, fotósorrendet, lomtárat, megszakadt/nem duplikáló feltöltést, két kliens szinkronját, helyi quota exceeded hibát.
2. Teljesítménymérés 100/1000/5000 tárgy és több ezer thumbnail esetén. Memóriahasználat, letöltési sávszélesség, háttérbe tett alkalmazás visszanyitása, cache-limit, kisméretű thumbnail minősége. Célértékeket mérésből válassz; ne állíts iPhone-on mért teljesítményt Mac/iPhone teszt nélkül.
3. Biztonság: HTTPS, hitelesítés, megfelelő session/cookie/CSRF/CORS szabályok, hozzáférés-szabályozás minden fotón és tárgyon, véletlen publikáció kizárása, titkok és felhasználói fotók kizárása a verziókezelésből. Az iPhone-on tárolt privát leltáradatokra vonatkozó korlátokat írd le.
4. Tárolás: `navigator.storage.persist()` és kvótabecslés kezelése. Figyelmeztetés, ha kevés a hely; megfelelő takarítás kizárólag eldobható eredetikép-cache-ben (nem offline thumbnail, nem outbox!). Készíts **felhasználó által indítható helyi exportot** a még nem szinkronizált tárgyakból, módosításokból és fényképekből, import/restore lehetőséggel vagy ellenőrizhető, dokumentált visszaállítási eljárással. A mentés ne küldje kéretlenül felhőbe a leltárt.
5. Installálási útmutató Windows szerverhez és iPhone-hoz: biztonságos elérés és HTTPS-előfeltételek, Safari → Megosztás → Hozzáadás a Főképernyőhöz, első TELJES offline letöltés, állapot ellenőrzése, repülőgép mód teszt, kameraengedély, későbbi szinkron. Legyen képernyőn is megjelenő rövid tájékoztatás: „Az offline adatok nem helyettesítik a biztonsági mentést.”
6. Magyar, felhasználóbarát hibák és hozzáférhetőség (érintés, kontraszt, címkék, állapotfeliratok). Egyértelmű „Minden adat és előnézet offline elérhető” állapot csak teljes ellenőrzés után.
7. Részletes végső fejlesztői és felhasználói átadás: architektúra, adatmodell, szinkron API, konfliktusfolyamat, kockázatok, iOS korlátok, visszaállítási eljárás, manuálisan elvégzendő iPhone-tesztlista.

## Elfogadási feltételek
- Repülőgép módban az összes előre szinkronizált tárgy és minden kis előnézet megjelenik.
- Kizárólag online és az adott kép megnyitásakor jön teljes felbontás; offline mindig van megfelelő előnézet, ha az adat egyszer sikeresen le lett töltve.
- Új, nem szinkronizált tárgyak és eredeti képek biztonságosan exportálhatók; igazolható visszaállítási próba.
- A szinkronizálás sikere és hibája ellenőrizhető; nem veszít el adatot hálózati ingadozásnál.
- A kód és az üzemeltetési dokumentáció alkalmas arra, hogy a felhasználó valós iPhone készüléken manuálisan teszteljen.

## Kötelező külön iPhone acceptance-folyamat
- Dokumentáld a pontos, **egyetlen kanonikus HTTPS URL-t**, amelyről a PWA telepítve és amelyhez az IndexedDB tartozik. Telepítés Safari-ból, iPhone 12 Pro készüléken, Tailscale aktív, majd repülőgép mód/Tailscale kikapcsolás: az alkalmazás offline shell + minden rekord + minden bélyegkép betölthető. Visszakapcsoláskor online teljes kép csak megnyitáskor töltődjön.
- Ellenőrizd kézzel: app kilövése/újraindítás, iPhone újraindítás, szabad tárhely csökkenése, fotó készítése kamera/galéria útvonalon, HEIC és álló/fekvő orientáció, 20 MiB képhatár, 200 MiB mentési határ, VPN bontása feltöltés közben, konfliktus a PC-ről, verzióváltás után offline adat megmaradása.
- **Kulcsteszt:** offline készíts 10 tárgyat 20 eredeti fotóval; alkalmazás kilövése után is helyben megmaradjanak. Szinkronnál megszakított válasz és újrapróbálkozás után sem lehet duplikáció, elveszett eredeti vagy hamis „kész” státusz.
- A backup ne legyen puszta exportgomb: az állomány tartalmazza az unsynced outboxot és fotóblobokat, import teszttel igazold a visszaállíthatóságot. PWA eredetváltás / újratelepítés / iOS storage eviction következményeit magyarázd el a felhasználónak.
- Ne változtasd meg automatikusan a Windows szerver bootolását, router-, tűzfal- vagy Tailscale-konfigurációját; külön manual lépések és rollback útmutató kell. Különböztesd meg a WebKit által támogatott működést az eszközön még ki nem próbált hipotézisektől.

## Regressziós ellenőrzés és fáziskapu
- A 04-ben **már meglévőnek** kell lennie az 01-es biztonságos kapcsolati/hitelesítési modellnek, a 02-es snapshot+napló rétegnek és a 03-as outbox+idempotens feltöltés+mentés/import rétegnek. Ha bármelyik hiányzik, a 04 nem adhat „kész” átadást; vissza kell jeleznie a hiányzó fázist.
- A valódi iPhone használatának megkezdése előtt (nem csak az utolsó fázisban) legyen ellenőrzött személyesadat-/fénykép-mentési terv a szerver `.local/personal` vagy Docker köteteinek visszaállítható biztonsági mentésére is. A README szerint a jelenlegi verzióban *nincs külön backup*; a PWA nem helyettesíti ezt.
- Az 01-es fázis iPhone telepítési és HTTPS-próbája legyen dokumentált, a 02-ben az offline teljes leltár kézi ellenőrzése, a 03-ban az offline új fotó és visszatöltés ellenőrzése; a 04 záró átvételi teszt, nem az első iPhone-próba.
- Az első négy feladatban szereplő „szinkron” szót pontosan használd: 02 = online read-side letöltés/inkrementális frissítés; 03 = valódi kétirányú offline szerkesztés és felküldés. A UI állapot ne állítsa, hogy minden szinkronizált, ha helyi függő művelet maradt.
