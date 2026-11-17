# Egységes feladatstátuszok

Hiteles forrás: a `main` ág. A szabály a promptok és roadmap-tervek megvalósítását jelöli; egy terv megírása még nem implementáció.

| Státusz | Jelentés | Továbblépés bizonyítéka |
|---|---|---|
| Tervezett | Rögzített cél vagy ötlet; még nincs igazolt, megkezdett megvalósítás | A feladat tényleges fejlesztésének kezdete |
| Fejlesztés alatt | A megvalósítás elkezdődött, részben kész vagy javításra visszaadott | Kész feladatkör, releváns sikeres ellenőrzések, elérhető tesztkörnyezet és átadási útmutató |
| Tesztelhető | A kijelölt feladatkör a felhasználó számára ténylegesen kipróbálható a `teszt` ágon | A felhasználó visszajelzése és az elfogadási feltételek teljesülése |
| Elfogadott | A szükséges felhasználói elfogadás igazolt, a feladat lezárható | Lezárás és archiválás a `main` ágon, a bizonyítékok megőrzésével |

Normál sorrend: Tervezett → Fejlesztés alatt → Tesztelhető → Elfogadott. Hibánál vissza lehet lépni Fejlesztés alatt állapotra, az ok és a korábbi tesztverzió megőrzésével.

## Kötelező adatlap

- **Státusz:** a fenti négy érték egyike.
- **Pontosítás / akadály:** például ötlet, vizsgálat szükséges, előfeltétel hiányzik, asztali eredmény nem ellenőrizhető. Ez nem külön életciklus-státusz.
- **Megvalósítás:** ág és teljes commit-hivatkozás; ha nincs igazolt megvalósítás, ezt írd le.
- **Ellenőrzés:** eredmény, dátum, az ellenőrzött commit és a riport hivatkozása. Különítsd el a korábbi riportból átvett és az újra lefuttatott ellenőrzést.
- **Felhasználói teszt:** nincs átadva / folyamatban / elfogadva dátummal és visszajelzési hivatkozással; szükség esetén a tesztverzió címkéje.
- **Frissítve:** dátum, Magyarország szerinti nap.

Az ismeretlen eredmény legyen „nem igazolt”, ne „sikeres”. A branch neve önmagában nem bizonyíték. Egyszerű tervezési/munkarendi dokumentumokhoz elegendő a dokumentáció ellenőrzése és a felhasználó kifejezett jóváhagyása; ezek miatt ne állíts elő alkalmazás-tesztkiadást.

## Frissítés és lezárás

A fejlesztő feladatkezdéskor, érdemi állapotváltozáskor és átadáskor frissíti az adatlapot a `main` ágon. A hozzá tartozó kód és verziófüggő riport a fejlesztési ágon marad. Ez munkavégzéshez kötött frissítés, nem időzített háttérfolyamat.

Összetett roadmapnél az összes leírt cél számít: egy részfeladat sikere nem teszi az egész tervet Tesztelhetővé vagy Elfogadottá. Kötelező eszközpróba vagy hozzáférés hiányában ne archiválj.

A közös útmutatók (`00-KOZOS-UTASITASOK.md`, indexek és sablonok) nem végrehajtandó feladatok, ezért nem kapnak megvalósítási státuszt.

A [projektállapot](ALLAPOT.md) összefoglalja az adatlapokat, nem vezet külön életciklust.
