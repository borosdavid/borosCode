# Snake – helyi játék közös szerveres ranglistával

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** Ötlet; részletes specifikáció és megvalósítás nincs.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


**Felületek:** web, iOS és Android
**Rögzítve:** 2026-11-17

## Cél

A [közös alkalmazásgyűjtő](ALKALMAZASGYUJTO-KEZDOKEPERNYO.md) egyik csempéjéről indítható Snake játék. A játékmenet teljes egészében a felhasználó eszközén fusson. A szerver a közös ranglistát (leaderboardot) kezelje; ne kelljen hozzá folyamatos kapcsolat a játék alatt.

## Helyi játékmenet

- Helyben fusson a mozgás, az irányítás, az étel megjelenítése, az ütközésvizsgálat és a pontszámítás.
- A már letöltött/telepített játék szerver és internet nélkül is induljon el, lehessen játszani és új játékot kezdeni.
- Weben billentyűzetes, telefonon érintéses irányítás szükséges; az érintéses megoldás pontos formája később választandó ki.
- A helyi és az összesített rekord megjelenítését egyértelműen különítsük el. A ranglista elérhetetlensége ne szakítsa meg és ne lassítsa a játékot.
- A közös kezdőképernyőre vissza lehessen térni; a játék ne módosíthassa a leltár vagy a sportnapló adatait.

## Közös ranglista

- A szerver tárolja a beküldött eredményeket, és ugyanazt a ranglistát szolgálja ki weben, iOS-en és Androidon.
- Szerverkapcsolat csak az eredmény beküldéséhez és a ranglista frissítéséhez szükséges.
- Offline eredménynél legyen egyértelmű, hogy az még csak helyi pontszám. A későbbi feltöltés módja – kézi beküldés vagy automatikus újrapróbálás – nyitott döntés.
- Ha késleltetett/ismételt feltöltést választunk, ugyanaz a játszma ne jelenjen meg többször a ranglistán.
- A közös rangsorhoz összehasonlítható szabályok, játékváltozat és nehézség szükséges; ezeket részletes specifikációban kell rögzíteni.
- A kliens által közölt pontszám önmagában nem bizonyítja a szabályos játékot. A pontellenőrzés és a visszaélésvédelem szükséges szintjét a későbbi felhasználói kör alapján válasszuk meg; ne ígérjünk csalásbiztos ranglistát puszta pontszámfeltöltéssel.

## Nyitott döntések

- Háztartáson belüli vagy más, meghatározott felhasználói körrel közös ranglista.
- Felhasználói fiók, becenév, illetve az eredményhez kötelezően megjelenő adatok.
- Legjobb eredmény játékosonként vagy minden játszma; időszakos és/vagy összesített rangsor.
- Játéktér, sebesség, nehézség, szüneteltetés és telefonos irányítás.
- Helyi rekord és fel nem töltött eredmény megőrzése, törlése és szinkronizálása.
- Eredményellenőrzés, beküldési korlátok és adatvédelmi szabályok.

## A későbbi megvalósítás igazolása

- Weben és az alkalmazásokban ugyanazon szabályok szerint játszható.
- Korábbi letöltés/telepítés után repülő módban, a szerver leállítása mellett is működik.
- A különböző eszközök sikeres feltöltés után ugyanazt a szerveres ranglistát látják.
- Sikertelen vagy ismételt eredménybeküldés nem akadályozza a helyi játékot és nem duplázza a játszmát.
- A pontszámok megjelenítése külön jelöli a helyi és a szerver által visszaigazolt állapotot.

**Ez egy későbbi, önálló modul terve, nem jelenleg végrehajtandó fejlesztési feladat.**
