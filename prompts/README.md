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

0. **00A-HIBERNATE-JPA-ATALLAS.md** – JdbcTemplate → Hibernate / Spring Data JPA refaktor, a mobilos fejlesztések előtt.
1. **01-PWA-HTTPS-MOBIL-ALAP.md** – mobil és biztonságos elérés.
   - **[01A-FELHASZNALOI-FIOKOK.md](01A-FELHASZNALOI-FIOKOK.md)** – felhasználónév/jelszó, azonos teljes alkalmazásjog; a telefonos hozzáférés előfeltétele.
2. **02-TELJES-OFFLINE-LELTAR-ELOZETEK.md** – offline olvasás.
3. **03-OFFLINE-MODOSITAS-KAMERA-SZINKRON.md** – offline írás és szinkron.
4. **04-IPHONE-TESZT-ADATVEDELEM-ATADAS.md** – teszt és átadás.

A `00-KOZOS-UTASITASOK.md` szabályai minden fázisra érvényesek. A fázisok nem indulnak el automatikusan.

## Következő feladat – átmeneti Windows-tesztszerver

**[01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md](01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md)** – D-011 szerint átmenetileg a bekapcsolt Windows fejlesztőgép a tesztszerver; Debian későbbre kerül. Az izolált natív indítás, privát HTTPS-beállító és GitHub-műveleti ügynök dev-ben elkészült; 71 PowerShell assertion sikeres. A valódi Windows/Tailscale/iPhone és innen indított vezérlési próba még szükséges; Fejlesztés alatt, nem lezárt fázis. Ezt követi a 02/03 offline fejlesztés.

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

A 01B konkrét megoldási terve: [közös távoli szerverhozzáférés](../projekt/SZERVER-TAVOLI-HOZZAFERES.md). A javaslat külön kezeli a felhasználó helyi Windows-kezelését (később Debian SSH), a ChatGPT korlátozott műveleti csatornáját és az alkalmazásfiókokat.
