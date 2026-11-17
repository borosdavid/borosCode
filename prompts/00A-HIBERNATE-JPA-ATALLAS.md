# 00A – Áttérés Hibernate ORM + Spring Data JPA használatára

> **Státusz:** Fejlesztés alatt  
> **Pontosítás / akadály:** A JPA-kód elkészült, ellenőrizve és feltöltve a dev ágra. Külön tesztágas átadás és felhasználói elfogadás még nincs; a Docker-futtatás nem ellenőrzött, a dev meglévő PWA offline próbája nyitott.  
> **Megvalósítás:** dev; [ellenőrzött összeillesztés](https://github.com/borosdavid/borosCode/commit/dae1224627a983217296ffe2dd47a339caacdb59), benne az eredeti [JPA-commit](https://github.com/borosdavid/borosCode/commit/4ab60617c225714ba9cfdd7a19aa5d6fc0e0caea).  
> **Ellenőrzés:** 2026-11-17-én újrafuttatva: Maven verify, 10 egységteszt + 14 izolált MySQL-integráció és mind a 7 Windowsos acceptance csoport sikeres. A külön mock PWA-próbából 2 csoport sikeres, az offline deep-link csoport Edge alatt timeouttal leállt; [pontos dev riport](https://github.com/borosdavid/borosCode/blob/dae1224627a983217296ffe2dd47a339caacdb59/docs/jpa-dev-integracio.md).  
> **Felhasználói teszt:** Nincs átadva a teszt ágon; felhasználói elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


> **Futtatási sorrend:** az iOS/PWA fejlesztési fázisok (01–04) előtt, külön, ellenőrizhető refaktorálásként. A `prompts/00-KOZOS-UTASITASOK.md` adatmegőrzési, biztonsági és ellenőrzési szabályai erre is érvényesek. **Ez a fájl önmagában nem végrehajtási utasítás.**

## Cél
A meglévő Otthontár **Inventory** backend perzisztencia-rétegét állítsd át a jelenlegi `JdbcTemplate` alapú kézi SQL-ről **Hibernate ORM + Jakarta Persistence (JPA) + Spring Data JPA** használatára. Az alkalmazás külső viselkedése, a már létező MySQL-adatok, JPEG-fájlok, API-szerződések, verzióütközések és Windows/Docker indítás maradjon változatlan. Ez tanulási és hosszú távú karbantarthatósági célú refaktor, **nem új funkció**.

## A jelenlegi kód releváns pontjai
- `inventory/pom.xml`: Spring Boot 4.1.2 parent, Spring JDBC starter és Flyway/MySQL driver; ellenőrizd a **tényleges** dependency-feloldást, a Spring Boot adott kiadásához illeszkedő JPA startert és Hibernate verziót; ne rögzíts kézzel egy másik Hibernate verziót indokolatlanul.
- `Ledger.java`: `JdbcTemplate`, `Draft` / `Place` rekordok, `save`, `completeSale`, `transition`, `item`, `items`; `@Transactional` és verzióellenőrzés.
- `Actions.java`: `item_action` olvasás, felvétel, aktiválás/átnevezés.
- `Photos.java`: `photo` / `file_cleanup` táblák és fizikai képfájlok; @Scheduled takarítás.
- `Web.java`: meglévő REST/Thymeleaf végpontok és válaszok.
- `V1__inventory.sql`, `V2__item_actions.sql`: meglévő Flyway-adatbázisséma; a tényleges `location`, `item`, `photo`, `item_action`, `file_cleanup` táblák és idegen kulcsok az igazodási alap.

## Megvalósítási követelmények
1. **Kód- és sémaaudit:** először készíts rövid térképet az összes `JdbcTemplate` használatról és a jelenlegi API/SQL-viselkedésről. Vedd számításba a képek eredeti fájljait és a `Photos.store` meglévő tranzakció utáni fájltakarítását. Ha a repository már változott, a tényleges állapothoz igazodj.
2. **Függőségek:** Spring Data JPA starter a Spring Boot által menedzselt Hibernate-del, Jakarta Persistence entitásokkal. Flyway marad az egyetlen séma-migrációs eszköz. Éles és teszt konfigurációban `spring.jpa.hibernate.ddl-auto=validate` (vagy ennél szigorúbb, nem módosító stratégia); **tilos** `create`, `create-drop`, `update` sémagenerálást használni létező adatokon.
3. **Entitások:** modellezd a meglévő táblákat és pontos oszloptípusokat az aktuális MySQL-séma szerint. UUID-k `CHAR(36)` formában, megfelelő Java String/UUID leképezéssel; `DECIMAL(14,2)` → `BigDecimal`; dátumok, nullable mezők, foreign key-ek, `position_no`, `cover`, `trashed` és `active` helyes leképezése. **Ne hozz létre új táblát a Hibernate kedvéért.** Ha a Hibernate egy kulcs-/oszlopkorlát miatt nem tud megfelelni, ezt dokumentáld, és csak indokolt, adatmegőrző, új Flyway-migrációval változtass.
4. **Repositoryk és szolgáltatások:** vezess be jól elnevezett JPA repositorykat; a `Ledger`, `Actions`, `Photos` adatbázis-műveleteit és lekérdezéseit fokozatosan refaktoráld. A **végállapotban az alkalmazás saját üzleti adatelérésében ne maradjon JdbcTemplate**; ha speciális SQL kell, használj JPA által támogatott JPQL/native query/EntityManager megoldást, indokolással. A `file_cleanup` sor és ütemezett takarítás is kapjon JPA alapú megoldást.
5. **Válaszszerződés:** tartsd meg a meglévő REST útvonalakat, multipart `draft` JSON mezőneveket, hibakódokat, és a kliens által használt snake_case mezőket (`location_id`, `action_id`, `sold_date`, `destination_id`, `photos`, stb.). **Ne add vissza közvetlenül JPA entitásokat a REST API-ban**; használj explicit DTO-kat/mapperréteget, amely a korábbi JSON-t reprodukálja. A Thymeleaf és jelenlegi `inventory.js` változatlan felhasználói működése kötelező.
6. **Konkurencia:** a jelenlegi `item.version` optimistic locking szemantikáját és HTTP 409-et őrizd meg. Használható `@Version` csak akkor, ha a jelenlegi számozást (új tárgy induló verziója = 1; frissítéskor +1), zárolási feltételeket és `trashed`/állapotkorlátokat teljesen megtartja; különben explicit verziófeltételes repository query-ket alkalmazz. A `completeSale`, `transition` és `save` konkurens, érvénytelen vagy inaktív műveleteknél se változtassák a korábbi üzleti szabályokat. Azonos verziójú párhuzamos módosításból csak az egyik sikerülhet.
7. **Tranzakciók és fájlok:** a kép mentése MySQL + fájlrendszer + külön imaging HTTP-szolgáltatás együttműködésével történik; ezek nem lesznek automatikusan atomiak attól, hogy JPA-t használunk. Őrizd meg a rollback utáni képeltávolítást és a tartós `file_cleanup` sort, beleértve a hibás fájltörlés újrapróbálását. Figyelj a Hibernate flush időzítésére, hogy SQL hibák még a kompenzáló takarítás előtt kiderüljenek. Ne veszíts fényképet, ne hagyj új árvafájlt.
8. **Lekérdezési teljesítmény:** elkerülendő az N+1 fotó/helyszín lekérdezés és a véletlen lazy loading szerializáció. Indexek megtartása, szükség szerint projection/fetch join/batch, determinisztikus fotósorrend és listák. Teszteld a `trashed=true/false` listát és a `SOLD` rendezést.
9. **Elszigeteltség:** a külön `imaging` modul és HTTP API változatlan; Java 21 célszint, Maven, Docker Compose, Windows indítóscriptek, portok, naplózás és health végpontok maradjanak használhatók. Ne végezz mellékesen PWA- vagy offline-sync fejlesztést.
10. **Dokumentálás:** rövid `docs/` leírás az entitáskapcsolatokról, repositorykról, DTO vs entitás rétegről, Flyway/JPA szerepéről és a migrációs kockázatokról. A gyökér README-t frissítsd az új perzisztenciaréteg valós állapotára.

## Tesztelés és elfogadási feltételek
- Készíts **független, izolált tesztadatbázison** integrációs próbákat a jelenlegi Flyway V1/V2 (és ha van, további) sémával. **NE** futtass destruktív adatbázis-tesztet a `.local/personal` profilon vagy a személyes Docker volumeken.
- Minden meglévő végpont működjön változatlan szerződéssel: helyszín, műveletek, új tárgy, módosítás, eladás, költöztetés, lomtár, restore, purge, fotófeltöltés, borítókép, kis/eredeti JPEG, feldolgozási hiba.
- Teszteld külön az új tárgy első verzióját, az optimistic locking HTTP 409-et, a helyszín- és action-ütközéseket, az aktív/inaktív műveleteket, a rollback-et és a file_cleanup újraindítást.
- `mvn verify`, releváns meglévő acceptance tesztek és JPA/DB integrációs tesztek zöldek; ha valamelyik eszköz nem elérhető, a hiányt jelentsd, ne állíts hamis sikert.
- Végezz lehetőség szerint **read-only összehasonlítást** meglévő adatokon (régi és új DTO mezők, rekordszámok, sorbarendezés, képhivatkozások). Csak ellenőrizhető backup birtokában és explicit felhasználói jóváhagyással dolgozz éles adatbázison.
- Minden releváns SQL-művelet Hibernate/JPA használatával történik, miközben a kliens által érzékelt működés nem változik. Sorold fel a módosított fájlokat, migrációkat (ha voltak), teszteket és ismert kockázatokat.

## Codexnek adandó utasítás
Olvasd el a `prompts/00-KOZOS-UTASITASOK.md` és ezt a `prompts/00A-HIBERNATE-JPA-ATALLAS.md` fájlt, majd a tényleges repositoryt. A meglévő JDBC perzisztenciaréteget váltsd át Hibernate ORM + Spring Data JPA alapra a fenti adatmegőrzési és API-kompatibilitási követelményekkel. Fokozatos, tesztelhető változtatásokat végezz; különösen óvd a meglévő adatbázist és fényképeket. A PWA 01–04 fázisokhoz még ne nyúlj. Végül futtasd a releváns teszteket, és foglald össze az eredményt.
