# Asztali böngészős felület bővítése és dizájncsiszolás

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** A részletes cél a dokumentumban szerepel; a megvalósítás még nincs igazolva.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


**Prioritás:** az alapfunkciók, mobilos hozzáférés és adatbiztonság után

## Cél
Az Otthontár asztali böngészős felületének funkcionális és vizuális továbbfejlesztése a meglévő munkafolyamatok megőrzésével.

## Lehetséges irányok
- Egységesebb vizuális arculat: színek, tipográfia, térközök, ikonok, komponensek.
- Jobb navigáció a leltár, a költözések, az eladás, a naptár és a későbbi külön modulok között.
- Áttekinthetőbb, nagyobb kijelzőre optimalizált tárgylista, szűrés, rendezés és keresés.
- Részletesebb adatlapok, képgaléria, csoportos műveletek, ha valóban szükségesek.
- Reszponzív kialakítás, billentyűzetes használat és akadálymentesség (WCAG-szempontok).
- Következetes betöltési, mentési, hiba- és szinkronstátuszok.

## Kapcsolat a közös kezdőképernyővel

A [csempés alkalmazásgyűjtő](ALKALMAZASGYUJTO-KEZDOKEPERNYO.md) weben is alapértelmezett belépési felület lesz. Ez közös web/iOS/Android termékirány; az itt tervezett asztali dizájncsiszolás ehhez igazodik, miközben az Otthontár és a többi modul saját felülete megmarad.

## Korlátok
- Nem indokolt pusztán dizájn okán lecserélni a működő Thymeleaf/JavaScript frontend teljes technológiai alapját.
- A felület módosítása ne veszélyeztesse a meglévő adatokat és API-szerződéseket.
- Előbb mérjük fel a tényleges felhasználói problémákat, aztán határozzuk meg a részletes specifikációt.

**Ez roadmap-dokumentum, nem Codex-feladat.**
