# Codex-promptok – Otthontár

Ebben a mappában gyűjtjük a ChatGPT-vel előkészített, később a Codexnek átadható fejlesztési feladatokat.

## Használat

1. A feladatot először beszéljük át, majd mentsük ide egy külön, beszédes nevű Markdown-fájlban (például `001-uj-funkcio.md`).
2. Codexben nyisd meg a `borosCode` projektet, és kérd meg: „Olvasd el és valósítsd meg a `prompts/001-uj-funkcio.md` feladatát.”
3. A Codex előbb vizsgálja meg az aktuális kódot, és csak utána módosítson.
4. A változtatásokat és a teszteket ellenőrizd commit/push előtt.

A `SABLON.md` egy új feladat kiindulópontja, **nem végrehajtandó fejlesztési kérés**.

## Fejlesztési sorrend

0. **00A-HIBERNATE-JPA-ATALLAS.md** – JdbcTemplate → Hibernate / Spring Data JPA refaktor, a mobilos fejlesztések előtt.
1. **01-PWA-HTTPS-MOBIL-ALAP.md** – mobil és biztonságos elérés.
2. **02-TELJES-OFFLINE-LELTAR-ELOZETEK.md** – offline olvasás.
3. **03-OFFLINE-MODOSITAS-KAMERA-SZINKRON.md** – offline írás és szinkron.
4. **04-IPHONE-TESZT-ADATVEDELEM-ATADAS.md** – teszt és átadás.

A `00-KOZOS-UTASITASOK.md` szabályai minden fázisra érvényesek. A fázisok nem indulnak el automatikusan.

## Következő feladat – Windows-hozzáférés

**[01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md](01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md)** – a valódi Windows-szerver állapotának, naplóinak és fejlesztési riportjainak elérése ChatGPT-ből, biztonságos indítás/leállítás/újraindítás, majd a telefonos HTTPS-elérés igazolása. A felhasználó ezt jelölte ki következő feladatként, az offline fázisok előtt. Egyelőre előkészített feladat; a kapcsolat még nincs kiépítve.

## Alapelvek

- Egy fájl lehetőleg egy jól körülhatárolt feladatot írjon le.
- A prompt tartalmazzon célt, jelenlegi állapotot, elvárt működést, megkötéseket és ellenőrizhető elfogadási feltételeket.
- A meglévő személyes adatokat, képeket, helyi konfigurációt és Docker-köteteket meg kell őrizni.
- Meglévő Flyway-migrációt ne módosítson; ha kell, készítsen újat.
- Módosítás után a Codex számoljon be a változtatásokról és az elvégzett tesztekről.
