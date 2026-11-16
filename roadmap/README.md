# Otthontár – hosszú távú tervek

Ez a mappa az Otthontár alkalmazás **termékvízióját, fejlesztési irányait és még nem véglegesített ötleteit** gyűjti.

## Különbség a `prompts/` mappához képest

- **`roadmap/`** – mit és miért szeretnénk később fejleszteni; lehetőségek, alternatívák, függőségek, nyitott döntések. Nem végrehajtási utasítás.
- **`prompts/`** – konkrét, körülhatárolt, ellenőrizhető feladatok a Codexnek.

Az itt szereplő ötletek **nem automatikus fejlesztési megbízások**. Egy hosszú távú tervből akkor készül Codex-prompt, amikor a megvalósítás mellett döntünk.

## Kiinduló cél

Az Otthontár kezdetben egy magyar nyelvű, Windows-gépen futó, Java/Spring Boot alapú költözési leltár. Hosszabb távon olyan háztartási alkalmazássá bővíthető, amely több eszközről is használható, és a költözés után is hasznos marad.

## Tervterületek és prioritások

A sorrend irányadó, nem végleges ütemterv. A részletes tervek önálló Markdown-fájlokban szerepelnek.

### 1. Stabil backend és adatbiztonság
- Hibernate ORM + Spring Data JPA átállás – részletes Codex-prompt: [00A-HIBERNATE-JPA-ATALLAS.md](../prompts/00A-HIBERNATE-JPA-ATALLAS.md)
- Megbízható biztonsági mentés és visszaállítás; hozzáférésvédelem és jogosultságok

### 2. iOS Otthontár és offline-first működés
- iPhone PWA, teljes leltár és kis előnézetek offline, eredeti képek online igény szerint
- Offline fényképezés, szerkesztés, kétirányú szinkronizáció
- Codex-promptok: [01–04](../prompts/)

### 3. Android-verzió – az iOS után
- Azonos háztartási leltár használata Androidon, offline funkciókkal
- Elsőként a PWA újrahasznosíthatóságának vizsgálata; natív Android csak indokolt esetben
- Részletes terv: [ANDROID.md](ANDROID.md)

### 4. Költözési és eladási naptár
- Tervezett és **ténylegesen megtörtént** események dátumainak kezelése
- Költözés, eladás, átadás és más, tárgyakhoz kapcsolódó események
- Részletes terv: [KOLTOZESI-NAPTAR.md](KOLTOZESI-NAPTAR.md)

### 5. Asztali böngészős felület bővítése és dizájncsiszolása – távolabbi jövő
- Több modulhoz jobb navigáció, kibővített desktop funkciók és egységesebb vizuális megjelenés
- Részletes terv: [ASZTALI-FELULET.md](ASZTALI-FELULET.md)

### 6. Sport- és táplálkozásnapló – különálló, hosszú távú fejlesztési ág
- Sportolások és étkezések rögzítése naptárnézetben weben és mobilon
- Kalória- és tápértékadatok automatikus keresése lehetőleg ingyenes külső API-ból; a szolgáltatók feltételei ellenőrizendők
- **Önálló üzleti modul és architektúra**, nem a költözési leltár funkcióinak összekeverése
- Részletes terv: [SPORT-ES-TAPLALKOZAS.md](SPORT-ES-TAPLALKOZAS.md)

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

Roadmap létrehozva és bővítve: 2026-11-16.
