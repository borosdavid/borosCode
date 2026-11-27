# 01C – Az Otthontár webes felületének próbája

A megvalósítás a [`50aa09b` main commit 01C promptjára](https://github.com/borosdavid/borosCode/blob/50aa09b2bdb119d5dfa029e9cd64db6b36715cfe/prompts/01C-WEB-FELULET.md) épül.

Az asztali oldalsáv és a mobilos navigáció ugyanazokat a leltárnézeteket éri el. A képernyő tetején a tárgyak, helyszínek és borítóképes tárgyak száma látszik. A **Fotónézet** nagyobb előnézeteket használ, a **Lista** több tárgyat fér el egymás alatt. A belépőképernyő külön, rajzolt költözési borítót kapott. A név és helyszín szerinti keresés ékezet nélkül is működik, a helyszín- és eladásszűrővel együtt. A bejelentkezés, az adatlap, a fotók, a beállítások és az offline indítóoldal közös stílust használ.

## Előkészítés

A műveleteket külön tesztfiókkal és elkülönített adatbázissal, fotókönyvtárral végezd. Helyi Windowson az elkülönített példányhoz lásd a [01B indítási útmutatót](windows-tesztszerver-01b.md). Ubuntu alatt a tesztre jóváhagyott commit telepítési lépéseit az [Ubuntu útmutató](KNOWHOW-ubuntu-frissites.md) tartalmazza. A szerverfrissítés előtt egyeztesd, hogy az elért példány melyik commitot futtatja.

## Kézi ellenőrzés

| Nézet | Próba | Elvárt eredmény |
|---|---|---|
| Belépés | Nyisd meg az elkülönített példány leltárát, lépj be tesztfiókkal, majd lépj ki | A belépő, a leltár és a kijelentkezés útvonala az alkalmazás előtagján belül marad |
| Tárgylista | Válts fotónézet és lista között, keress ékezet nélkül, válassz helyszínt és próbáld az eladásszűrőt | A választott nézetet újratöltés után is megjegyzi a böngészőlap. A szűrők együtt működnek, az eredmények száma látható |
| Adatlap | Hozz létre egy fotós próbatárgyat, mentsd, nyisd meg újra, válassz borítóképet | Az előnézet a listában megjelenik, az adatlapon megnyitható az eredeti kép |
| Műveletek | Rögzíts eladást és hajts végre költöztetést külön próbatárgyakon. Tegyél lomtárba és állíts vissza egy teszttárgyat | Minden művelet a meglévő szabályok szerint működik, a változás látható a listán |
| Elrendezés és fókusz | Próbáld ki asztali böngészőben és kb. 390 px szélességen. Használd a Tab billentyűt és nagyítsd a szöveget | A vezérlők elérhetők, a fókusz követhető, a tartalom vízszintes görgetés nélkül olvasható |
| PWA | Az elkülönített HTTPS-példányt telepítsd iPhone Safariból. Repülőgép módban nyisd meg a telepített felületet | Az offline indítóképernyő a közös stílussal jelenik meg. A teljes offline leltár a 02 feladata |

Az automatizált `npm test --prefix tests` a helyi gyökérútvonal és az `/otthontar/` előtag alatt is lefuttatja a böngészőpróbákat, külön tesztadatokkal. A tesztek ellenőrzik a fotó-előkészítést, a munkamenet- és mentési hibák kezelését, a PWA cache frissítését és az offline indítást.

Felületváltás után a PWA az új verziót a már nyitott lapok bezárása után aktiválja. Mentsd a szerkesztést, zárd be az Otthontár-lapokat, majd nyisd újra az alkalmazást. Az alkalmazásverzió visszaváltható a korábbi ismert jó commitra. A 01C nem tartalmaz adatbázis-migrációt.
