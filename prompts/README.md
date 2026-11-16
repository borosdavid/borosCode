# Codex-promptok – Otthontár

Ebben a mappában gyűjtjük a ChatGPT-vel előkészített, később a Codexnek átadható fejlesztési feladatokat.

## Használat

1. A feladatot először beszéljük át, majd mentsük ide egy külön, beszédes nevű Markdown-fájlban (például `001-uj-funkcio.md`).
2. Codexben nyisd meg a `borosCode` projektet, és kérd meg: „Olvasd el és valósítsd meg a `prompts/001-uj-funkcio.md` feladatát.”
3. A Codex előbb vizsgálja meg az aktuális kódot, és csak utána módosítson.
4. A változtatásokat és a teszteket ellenőrizd commit/push előtt.

A `SABLON.md` egy új feladat kiindulópontja, **nem végrehajtandó fejlesztési kérés**.

## Alapelvek

- Egy fájl lehetőleg egy jól körülhatárolt feladatot írjon le.
- A prompt tartalmazzon célt, jelenlegi állapotot, elvárt működést, megkötéseket és ellenőrizhető elfogadási feltételeket.
- A meglévő személyes adatokat, képeket, helyi konfigurációt és Docker-köteteket meg kell őrizni.
- Meglévő Flyway-migrációt ne módosítson; ha kell, készítsen újat.
- Módosítás után a Codex számoljon be a változtatásokról és az elvégzett tesztekről.
