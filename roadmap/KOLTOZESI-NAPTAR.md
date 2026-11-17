# Költözési és eladási eseménynaptár

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** A részletes cél a dokumentumban szerepel; a megvalósítás még nincs igazolva.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


**Cél:** az Otthontárban követhető legyen, hogy melyik költözési, eladási vagy más tárgyhoz kapcsolódó esemény mikor **fog történni**, illetve mikor **történt meg**.

## Funkcióelképzelések
- Naptárnézet (havi, heti, napi vagy lista) tervezett és lezajlott eseményekkel.
- Tárgyhoz vagy tárgycsoporthoz kapcsolható események: költözés, szállítás, átadás, eladás, átvétel és egyéb egyedi művelet.
- Tervezett és tényleges dátum/idő külön nyilvántartása; terv módosítása után is látható maradjon, mikor valósult meg az esemény.
- Állapotok: tervezett, folyamatban, lezajlott, elmaradt/lemondott (végleges üzleti szabály később).
- Szűrés eseménytípus, helyszín, tárgy és állapot alapján.
- Kattintásból érintett tárgyak adatlapjainak megnyitása; később értesítés vagy emlékeztető vizsgálata.
- Mobilon és asztali böngészőben is használható naptár, az offline-first elvekkel összehangolva.

## Fontos adatmodellezési kérdések
- Az esemény önálló entitás legyen stabil azonosítóval, ne pusztán az `item` aktuális állapotának dátummezője.
- Egy esemény több tárgyhoz is tartozhat; egy tárgyhoz több esemény kapcsolódhat.
- A tervezett és tényleges időpont **külön mező**; az eseményelőzmények/audit és módosítási konfliktusok később tisztázandók.
- Időzónák, egész napos események és visszamenőleges rögzítés pontos szemantikája.
- A meglévő tárgyműveletek (különösen `MOVE`, `SALE`, `completeSale`) nem sérülhetnek; a naptáresemény és a tárgy tényleges állapota között tudatos kapcsolat kell.
- Offline létrehozás/módosítás, más eszközökkel való szinkron és ütközéskezelés.

## Későbbi döntések
Egyedi eseménytípusok, értesítési igény, ismétlődés, külső naptárintegráció és a lezajlott események naplózásának részletei.

**Ez roadmap-dokumentum, nem Codex-feladat.**
