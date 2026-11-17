# Külön fejlesztési ág: sportolás- és táplálkozásnapló

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** Távlati terv; megvalósíthatósági vizsgálat és részletes döntések szükségesek.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


**Függetlenség:** az Otthontár meglévő leltármoduljától **különálló fejlesztésként**, önálló domainmodellel, szerveroldali logikával és felülettel; megvizsgálható, hogy azonos portálon és közös hitelesítéssel jelenjen-e meg.

## Cél
A weboldal és mobilalkalmazás kibővítése olyan funkciókkal, amelyekben naptárszerű felületen rögzíthetők a sportolások és az étkezések; az élelmiszerek kalória- és tápértékadatait lehetőleg **ingyenes külső API-ból vagy automatizált kereséssel** kapjuk meg, ne kelljen mindent kézzel megadni.

## Sportnapló
- Naptárnézetben sporttevékenység rögzítése dátummal és időponttal.
- Tevékenységtípus (pl. kerékpározás, futás, edzőterem, séta, egyéb), időtartam, opcionális távolság és megjegyzés.
- Korábbi sportolások visszakeresése, heti/havi összesítések.
- Később opcionális energiaköltség-becslés; jelölni kell, hogy ez becslés, nem mért érték.

## Étkezés- és tápértéknapló
- Étkezések rögzítése időponttal; reggeli/ebéd/vacsora/nassolás vagy szabad kategória.
- Élelmiszerek/ételek keresése névvel, márkával, és később opcionálisan vonalkóddal.
- Adagméret és mértékegység alapján számított kalória (kcal), fehérje, szénhidrát, zsír; opcionálisan rost, cukor, só és további rendelkezésre álló tápanyagok.
- Egy étkezés több összetevőből állhat; saját receptek, kedvencek, előzmények későbbi lehetőségként.
- Napi/heti összegzések naptárnézetben; a kézzel javított tápértékek és források megőrzése.
- A hiányzó vagy bizonytalan tápértékadatot láthatóan jelezni kell; ne töltsük fel hallgatólagosan kitalált számokkal.

## Külső adatok – megvizsgálandó lehetőségek
- Ingyenes élelmiszer-/tápérték-adatbázisok API-hozzáféréssel, például **Open Food Facts**, valamint más nyílt vagy ingyenes kerettel elérhető források. Ezek **jelöltek**, nem jóváhagyott vagy korlátlan szolgáltatások.
- Tisztázandó az API-k aktuális használati feltétele, licencelés, attribúció, naprakészség, magyar termékek lefedettsége, adatminősége, sebessége és esetleges rate limitje.
- Automatizált webes keresés/crawling csak akkor, ha a forrás felhasználási feltételei és a vonatkozó szabályok lehetővé teszik; a strukturált, hivatalos API előnyben részesítendő.
- Több forrás esetén egységes belső élelmiszeradat-modell, eredet- és lekérdezési időbélyeg, normalizált mértékegységek, duplikátumkezelés és cache.
- Személyes étkezési/sportolási adatokat ne továbbítsunk harmadik félnek szükségtelenül; ahol lehet, csak keresési/termékadatokat kérjünk le, nem teljes személyes naplókat.

## Technikai elkülönítés
- Saját domainentitások és adatszolgáltatások: `Activity`, `Meal`, `Food`, `Serving`, `NutrientSource` – csak szemléltető elnevezések.
- Egyedi REST API és külön, verziózott adatbázis-migrációk; később döntendő el, hogy azonos adatbázis külön sémája vagy önálló adatbázis a jobb.
- Külön navigációs modul / mobilnézet; a leltár üzleti logikájába ne keveredjen.
- Offline adatfelvétel és későbbi szinkron lehetősége vizsgálandó az Otthontár infrastruktúrájának újrahasznosításával.
- Hozzáférésvédelem, biztonsági mentés, adatminimalizálás és biztonságos törlés már a tervezési szakaszban.

## Nyitott kérdések
1. Egy közös Otthontár alkalmazás külön modulja legyen-e, vagy külön termék ugyanazzal a backend infrastruktúrával?
2. Első körben kizárólag kézi adatbevitel + automatikus élelmiszer-keresés, vagy külső sportadatimport is kell?
3. Mely tápértékek fontosak, milyen mértékegység/adagkezelés szükséges?
4. Mekkora offline adatmennyiségre és mely mobilplatformokra lesz szükség?
5. Melyik ingyenes adatforrás megfelelő ténylegesen a magyarországi termékekhez?

**Ez roadmap-dokumentum, nem Codex-feladat; megvalósítás előtt külön specifikáció készül.**
