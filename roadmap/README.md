# Otthontár – hosszú távú tervek

Ez a mappa az Otthontár alkalmazás **termékvízióját, fejlesztési irányait és még nem véglegesített ötleteit** gyűjti.

## Különbség a `prompts/` mappához képest

- **`roadmap/`** – mit és miért szeretnénk később fejleszteni; lehetőségek, alternatívák, függőségek, nyitott döntések. Nem végrehajtási utasítás.
- **`prompts/`** – konkrét, körülhatárolt, ellenőrizhető feladatok a Codexnek.

Az itt szereplő ötletek **nem automatikus fejlesztési megbízások**. Egy hosszú távú tervből akkor készül Codex-prompt, amikor a megvalósítás mellett döntünk.

## Kiinduló cél

Az Otthontár kezdetben egy magyar nyelvű, Windows-gépen futó, Java/Spring Boot alapú költözési leltár. Hosszabb távon több eszközön használható, különféle funkciókat összefogó alkalmazásgyűjtővé bővülne. Weben, iOS-en és Androidon közös, csempés kezdőképernyő nyílna meg, ahonnan az Otthontár leltár, a naptár, a sport- és táplálkozásnapló, a játékok és a későbbi modulok érhetők el. Az Otthontár így a közös megoldás egyik önálló modulja maradna.

## Elkészült leírások

A teljesen elkészült feladatok leírásai a **[kesz/](kesz/README.md)** almappába kerülnek automatikusan, a fejlesztés lezárásakor. A részben elkészült vagy blokkolt feladatok itt maradnak. Áthelyezéskor a státuszokat, az indexeket és az érintett hivatkozásokat is frissítjük; a részletes szabályokat a gyökér [AGENTS.md](../AGENTS.md) tartalmazza.

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

### 7. Közös alkalmazásgyűjtő és csempés kezdőképernyő
- Weben, iOS-en és Androidon alapértelmezett kezdőképernyő, csempékről indítható önálló funkciókkal.
- Kezdeti modulok: Otthontár, Naptár, Sport- és táplálkozásnapló, Snake; később további funkciók.
- Közös navigáció és arculat, elkülönített moduladatok és működés; a meglévő leltár megőrzése.
- Részletes terv: [ALKALMAZASGYUJTO-KEZDOKEPERNYO.md](ALKALMAZASGYUJTO-KEZDOKEPERNYO.md)

### 8. Snake játék közös ranglistával
- Teljesen helyben futó játékmenet weben és az alkalmazásokban; már telepítve offline is használható.
- Közös szerveres leaderboard, online eredménybeküldéssel és ranglistalekéréssel.
- A ranglista elérhetetlensége nem akadályozza a játékot; a feltöltés és pontellenőrzés részletei még tervezendők.
- Részletes terv: [SNAKE.md](SNAKE.md)

Az új pontok sorszáma nem kötött megvalósítási sorrend: a közös kezdőképernyő az önálló modulok belépési kerete lehet. Az új termékirány nem módosítja a jelenleg következő Windows-hozzáférési feladatot.

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

Roadmap létrehozva: 2026-11-16. Legutóbbi bővítés: 2026-11-17 – közös alkalmazásgyűjtő és Snake.
