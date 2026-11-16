# Otthontár – hosszú távú tervek

Ez a mappa az Otthontár alkalmazás **termékvízióját, fejlesztési irányait és még nem véglegesített ötleteit** gyűjti.

## Különbség a `prompts/` mappához képest

- **`roadmap/`** – mit és miért szeretnénk később fejleszteni; lehetőségek, alternatívák, függőségek, nyitott döntések. Nem végrehajtási utasítás.
- **`prompts/`** – konkrét, körülhatárolt, ellenőrizhető feladatok a Codexnek.

Az itt szereplő ötletek **nem automatikus fejlesztési megbízások**. Egy hosszú távú tervből akkor készül Codex-prompt, amikor a megvalósítás mellett döntünk.

## Kiinduló cél

Az Otthontár kezdetben egy magyar nyelvű, Windows-gépen futó, Java/Spring Boot alapú költözési leltár. Hosszabb távon olyan háztartási alkalmazássá bővíthető, amely több eszközről is használható, és a költözés után is hasznos marad.

## Tervterületek

### 1. Stabil backend és adatbiztonság
- Hibernate ORM + Spring Data JPA átállás – részletes prompt: [00A-HIBERNATE-JPA-ATALLAS.md](../prompts/00A-HIBERNATE-JPA-ATALLAS.md)
- Megbízható biztonsági mentés és visszaállítás
- Hozzáférésvédelem, jogosultságok, ellenőrizhető adatmegőrzés

### 2. Mobilos, offline-first Otthontár
- iPhone-ra telepíthető PWA Mac és App Store nélkül
- Teljes leltár offline, minden kép kis előnézetével
- Teljes méretű kép igény szerinti online letöltése
- Offline fotózás, módosítás és kétirányú szinkronizáció
- Fejlesztési promptok: [01–04](../prompts/)

### 3. További háztartási modulok
- Később meghatározandó modulok és funkciók, a tényleges igények alapján
- A meglévő leltár megtartása és a közös navigáció bővítése

## Tervezési státuszok

- **Ötlet** – még nem döntöttük el, hogy szükséges-e.
- **Vizsgálat alatt** – megvalósíthatóság, költség, kockázatok és alternatívák tisztázása.
- **Tervezett** – a cél eldőlt, de a részletes specifikáció még nem készült el.
- **Prompt elkészült** – a megvalósítás Codex-promptja külön, a `prompts/` mappában van.
- **Fejlesztés alatt / elkészült** – csak tényleges kód- és tesztellenőrzés alapján jelölhető.

## Elvek

1. A jelenlegi működés és a személyes adatok biztonsága elsőbbséget élvez.
2. A roadmap nem tükröz automatikusan kész funkciókat; a tényleges állapotot a kód és a tesztek igazolják.
3. A döntéseket, előfeltételeket, kockázatokat és nyitott kérdéseket dokumentáljuk.
4. A megvalósítási sorrendet az egymásra épülő követelmények és a felhasználói prioritások határozzák meg.
5. A hosszú távú elképzeléseket különválasztjuk az azonnal végrehajtható Codex-feladatoktól.

Utolsó alapítási dátum: 2026-11-16.
