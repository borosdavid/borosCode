# Codex-promptok – Otthontár

Ebben a mappában gyűjtjük a ChatGPT-vel előkészített, később a Codexnek átadható fejlesztési feladatokat.

## Használat

1. A feladatot először beszéljük át, majd mentsük ide egy külön, beszédes nevű Markdown-fájlban (például `001-uj-funkcio.md`).
2. Codexben nyisd meg a `borosCode` projektet, és kérd meg: „Olvasd el és valósítsd meg a `prompts/001-uj-funkcio.md` feladatát.”
3. A Codex előbb vizsgálja meg az aktuális kódot, és csak utána módosítson.
4. A változtatásokat és a teszteket ellenőrizd commit/push előtt.

A `SABLON.md` egy új feladat kiindulópontja, **nem végrehajtandó fejlesztési kérés**.

## Elkészült leírások

A teljesen elkészült feladatok leírásai a **[kesz/](kesz/README.md)** almappába kerülnek automatikusan, a fejlesztés lezárásakor. A részben elkészült vagy blokkolt feladatok itt maradnak. Áthelyezéskor a státuszokat, az indexeket és az érintett hivatkozásokat is frissítjük; a részletes szabályokat a gyökér [AGENTS.md](../AGENTS.md) tartalmazza.

## Fejlesztési sorrend

1. [00A – Hibernate/JPA-átállás](00A-HIBERNATE-JPA-ATALLAS.md) – a meglévő perzisztenciaalap.
2. [01 – PWA és HTTPS](01-PWA-HTTPS-MOBIL-ALAP.md) és [01A – alkalmazásfiókok](01A-FELHASZNALOI-FIOKOK.md) – a használatban lévő webes alap és a hozzá tartozó PWA-ellenőrzések.
3. **[01C – Letisztult webes felület](01C-WEB-FELULET.md)** – a felület implementációja a `dev` ágon elkészült ([a2982a4](https://github.com/borosdavid/borosCode/commit/a2982a4e90baea4194db17bd8eb0c88fb8613f7d)); a publikus bejelentkezőn már látható a 01C, de a verziózott CSS/JS-erőforrások 404-es problémája és a teljes felhasználói/tesztágas átvétel még nyitott. [Közös konzisztencia-vizsgálat](https://github.com/borosdavid/borosCode-knowledge/blob/main/reviews/2026-11-24-KONZISZTENCIA.md).
4. [02 – Teljes offline leltár és előnézetek](02-TELJES-OFFLINE-LELTAR-ELOZETEK.md) – offline olvasás az új felületre építve.
5. [03 – Offline módosítás, kamera és szinkron](03-OFFLINE-MODOSITAS-KAMERA-SZINKRON.md) – offline írás és szinkron.
6. [04 – iPhone-teszt és átadás](04-IPHONE-TESZT-ADATVEDELEM-ATADAS.md) – eszközös átvétel.

A [közös szabályok](00-KOZOS-UTASITASOK.md) minden fázisra érvényesek. A fázisok külön utasításra indulnak.

## Jelenlegi elérés

A webes alkalmazás címe `https://boroscode.com/otthontar/` (Cloudflare és alkalmazásfiók). A szerver privát üzemeltetési útvonala iPhone-ról iSH → SSH a Tailscale hálózatán. A [01B Windows-tesztkörnyezet](01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md) külön feladat, saját elfogadási feltételekkel. A 01C és az Ubuntu/Cloudflare útvonalra épülő iPhone-fázisok ennek aktiválása nélkül haladhatnak.

## Alapelvek

- Egy fájl lehetőleg egy jól körülhatárolt feladatot írjon le.
- A prompt tartalmazzon célt, jelenlegi állapotot, elvárt működést, megkötéseket és ellenőrizhető elfogadási feltételeket.
- A meglévő személyes adatokat, képeket, helyi konfigurációt és Docker-köteteket meg kell őrizni.
- Meglévő Flyway-migrációt ne módosítson; ha kell, készítsen újat.
- Módosítás után a Codex számoljon be a változtatásokról és az elvégzett tesztekről.

## A tervek hiteles forrása: main

A `prompts/` és `roadmap/` aktuális tartalmát kizárólag a [main ágon](https://github.com/borosdavid/borosCode/tree/main) szerkesztjük és mentjük, beleértve a `kesz/` archívumokat is. A `dev` és `teszt` ágakon korábbi példányok maradhatnak, és normál összevonással frissülhetnek; automatikus szinkron nincs. Fejlesztéskor mindig a `main` aktuális promptját olvassuk, a megvalósítás viszont a `dev` ágra kerül. Státuszfrissítéskor jelöljük a kód ágát és commitját, a teszteredményeket és a hátralévő felhasználói próbát. A műszaki és átadási dokumentáció a kód verzióját követi. Részletek: [AGENTS.md](../AGENTS.md).

## Feladatstátuszok

Minden végrehajtandó prompt az [egységes státuszszabályokat](../projekt/STATUSZOK.md) és a [sablon adatlapját](SABLON.md) követi. A közös útmutatók és indexek nem végrehajtandó feladatok.

Aktuális összefoglaló: [projektállapot](../projekt/ALLAPOT.md).

Az aktuális elérést és a külön Windows-tesztkörnyezetet a [távoli hozzáférés leírása](../projekt/SZERVER-TAVOLI-HOZZAFERES.md) követi.
