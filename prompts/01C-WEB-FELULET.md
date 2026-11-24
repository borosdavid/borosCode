# 01C – Letisztult, reszponzív Otthontár webes felület

> **Státusz:** Fejlesztés alatt  
> **Pontosítás / akadály:** A visszajelzés után új belépőoldal, leltár-összesítő, fotónézet és kompakt listanézet készült. Az Ubuntu-példány frissítése, a címkézett tesztkiadás és az iPhone Safari-próba hátravan.  
> **Megvalósítás:** [`dev`, `a2982a4`](https://github.com/borosdavid/borosCode/commit/a2982a4e90baea4194db17bd8eb0c88fb8613f7d).  
> **Ellenőrzés:** 2026-11-24, `a2982a4`. A helyi Chromium-próba sikeres a gyökér- és az `/otthontar/` útvonalon, beleértve a belépő elrendezését és a nézetváltást. A CI Java/MySQL, Edge és PowerShell feladata sikeres. A CI Chromium-böngésző telepítése még futott a státusz rögzítésekor. [CI](https://github.com/borosdavid/borosCode/actions/runs/45352285164). [Kézi próbamenet](https://github.com/borosdavid/borosCode/blob/a2982a4e90baea4194db17bd8eb0c88fb8613f7d/docs/web-felulet-01c.md).  
> **Felhasználói teszt:** Nincs címkézett tesztágas átadás. A jelenlegi munkamenet GitHub-hozzáférése a szükséges annotált tag közzétételéhez nem elegendő.  
> **Frissítve:** 2026-11-24. [Státuszszabályok](../projekt/STATUSZOK.md).

## Cél

Alakíts ki nyugodt, áttekinthető és egységes webes felületet a költözéshez használt leltárhoz. Asztali gépen lehessen gyorsan áttekinteni és szerkeszteni sok tárgyat. Mobilböngészőben kényelmesen lehessen fotót hozzáadni, tárgyat keresni és szerkeszteni. A 02–04-es iPhone-fázisok erre a felületre épüljenek.

## Kiinduló helyzet

Az Otthontár címe [boroscode.com/otthontar/](https://boroscode.com/otthontar/). Az alkalmazásbelépés és a leltár jelenlegi működése adja a kiindulási alapot. Olvasd el a [közös szabályokat](00-KOZOS-UTASITASOK.md), a [PWA-alapot](01-PWA-HTTPS-MOBIL-ALAP.md) és a [fiókok leírását](01A-FELHASZNALOI-FIOKOK.md). Az aktuális `dev` kódban ellenőrizd a Thymeleaf-sablonokat, az `inventory.css` és `inventory.js` feladatait, valamint a tényleges útvonalakat és kezelőelemeket.

## Kialakítás

- Készíts egy következetes vizuális rendszert jól olvasható tipográfiával, tompa alapszínekkel, egy fő kiemelőszínnel, egységes térközökkel és jól látható aktív, fókusz-, hiba- és letiltott állapotokkal.
- Egységesítsd a magyar nyelvű bejelentkezés, tárgylista, adatlap és szerkesztés, fotógaléria, eladás, költöztetés, beállítások és lomtár felületét a már elérhető funkciók alapján. A listában a tárgy neve, előnézete, helyszíne és állapota legyen gyorsan áttekinthető.
- A meglévő keresést, szűrést és műveleteket helyezd el jól láthatóan. A gyakori feladatokhoz, például új tárgy felvételéhez és mentéséhez, legyen egyértelmű elsődleges gomb. A ritkább és törléssel járó műveletek helye és visszajelzése legyen következetes.
- Használj reszponzív elrendezést. A meglévő tartalom férjen el mobilon vízszintes görgetés nélkül, hosszú magyar címekkel, nagyított betűmérettel és hosszabb tárgylistán is. Legyen kezelhető érintéssel és billentyűzettel.
- A meglévő felületet és komponenseket építsd tovább. Őrizd meg a REST-szerződést, a belépést, a jelenlegi tárgyműveleteket, a fotók kezelését és a PWA működését. A stílushoz szükséges erőforrások működjenek a Cloudflare-es alútvonalon és az offline indítóképernyőn is. Az offline leltár és a szinkron a 02–04 feladata.

## Elfogadási feltételek

1. Asztali böngészőben és kb. 390 px széles mobilnézetben ellenőrizd a tárgylistát, a képes adatlapot és a szerkesztést. Próbáld ki billentyűzettel is a fókuszsorrendet és a vezérlők elérését.
2. Izolált tesztadattal járd végig a belépést, tárgy létrehozását, képfeltöltést, módosítást, a meglévő eladási és költöztetési műveleteket, a lomtárat és a kijelentkezést. Ellenőrizd az üres, betöltési és hibás állapotok érthetőségét.
3. Ellenőrizd a helyi futást és az `/otthontar/` alútvonalat. A stíluslapok, fotók, belső linkek, a belépés utáni átirányítás, a PWA manifest és az offline indítóképernyő működjenek a hozzájuk tartozó útvonalakon. Futtasd a releváns meglévő teszteket.
4. A felhasználói átadáshoz adj rövid próbamenetet asztalra és telefonra. Az átadás a `teszt` ág és a projekt [tesztverzió-eljárása](../projekt/TESZTVERZIOK.md) szerint történjen. Az ellenőrzött felület legyen a [02-es offline olvasás](02-TELJES-OFFLINE-LELTAR-ELOZETEK.md) kiindulópontja.
