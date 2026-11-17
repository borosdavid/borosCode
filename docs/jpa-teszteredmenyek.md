# 00A – Ellenőrzési jegyzőkönyv

Dátum: 2026-11-16, Europe/Budapest. Kiinduló GitHub main:
`ce9fdd60810f403c2f2145e032c50600209faa25`.

## Környezet és izoláció

- Windows, Maven 3.10.0; Java 25.0.5 és Java 21.0.4, Java 21 fordítási célszint.
- Spring Boot 4.1.2, Spring Data JPA 4.1.2, Hibernate ORM 7.4.13.Final; Maven
  dependency tree bizonyíték: `.local/jpa-dependencies.log`.
- MySQL 9.7.4. Az integrációs próbák saját natív MySQL-folyamatot és új,
  `inventory_jpa_test` sémát használnak. Nincs H2 vagy Hibernate sémagenerálás.
- A böngészős próba a külön `.local/acceptance` profilhoz csatlakozik:
  18740/18741 HTTP, 43316 MySQL, külön fotómappa. Node + Playwright Core + Edge.
- A személyes adatbázis, képek és Docker-kötetek nem voltak tesztcélpontok.

## Maven és integráció

| Ellenőrzés | Eredmény | Bizonyíték |
|---|---|---|
| Kiinduló `scripts/Build.ps1` | Sikeres, 3 egységteszt | `.local/jpa-baseline-build.log` |
| JPA `mvn -Pmysql-it verify`, `scripts/Test-Persistence.ps1` | Sikeres, 3 egységteszt + 14 MySQL-integráció, 0 hiba, 0 kihagyott teszt | `.local/jpa-integration.log`, `inventory/target/failsafe-reports/hu.boros.inventory.PersistenceIT.txt` |
| JPA `mvn verify`, Java 21 | Sikeres, 3 egységteszt, mindkét modul csomagolása és Spotless | `.local/jpa-java21-verify.log` |
| Google Java Format 1.35.0 / JDK 25 | Sikeres; formátumellenőrzés a verify része | `.local/jpa-format.log` |

Az integrációs futás részletes MySQL- és Maven-naplói, valamint saját adatkönyvtára:
`target/jpa-it/6b54eefa13914fb9acc9309ee66bb5af/`. A futtató a saját MySQL-t a végén
leállította. A naplók és generált adatbázisok helyben megmaradtak, Gitben figyelmen
kívül hagyott diagnosztikai fájlok.

A 14 `PersistenceIT` teszt az alábbiakat ellenőrzi:

1. Érintetlen V1/V2 sémába előzetesen beszúrt rekordok régi SQL-válaszának és új
   REST JSON-válaszának összehasonlítása: mezők, nullok, árak, dátumok, fotóhivatkozások,
   sorrend, aktív/lomtár lista; indulás előtti/utáni SHOW CREATE TABLE azonosság.
2. Helyszín- és műveletnév egyedisége, hiányzó id, hibás típus, az eredeti
   kérésfeldolgozási 400 és üzleti 422/409 viselkedés; eredeti SQL szerinti action-sorrend.
3. Új verzió 1, változtatáskor pontos +1, ismételt id és idegen kulcs ütközése 409;
   hiányzó művelet 422; új rekord nem merge-elhet egy korábbit.
4. Inaktív művelet megtartása korábbi tárgyon, új hozzárendelés tiltása, célhelyszín
   szabályai és kikapcsolt MOVE végrehajtása.
5. Eladás kötelező adatai, árvalidáció, állapotkorlátok; trash/restore/purge
   előfeltételek, verziók és a két lista elkülönítése.
6. Eredeti/előnézeti bájtok, fotósorrend, `new:N` és korábbi borító,
   fotóhozzáadáskor is pontosan egy verziónövelés, hiányzó/árva fotó 404.
7. Hibás borító vagy imaging 422/500 miatti teljes metadata-, fotó- és verziórollback;
   a feldolgozási hiba 503-ra fordítása, újrapróbálhatóság.
8. Már kiírt fotó kompenzáló törlése SQL flush idegenkulcshiba után.
9. Purge és sikertelen fájltörlés után a tartós sor megmarad; alkalmazás-újraindítás,
   a hiba megszüntetése és sikeres újrapróbálás.
10. Rollback alatti fájltörlési hiba után külön tranzakcióban tartós sor készül.
11. Azonos verziójú párhuzamos save/save közül pontosan egy 200, egy 409.
12. Párhuzamos save/sale, save/trash, save/move, restore/purge: egy siker, egy 409.
13. Az action PESSIMISTIC_READ zárolása blokkolja a konkurens kikapcsolást.
14. Hibernate statisztikával lista = 1, adatlap = 2 SQL; több tárgy mellett sincs
   N+1, JSON-szerializáláskor nincs lazy adatbázis-lekérdezés.

A tesztek által szándékosan kiváltott egyediségi, idegenkulcs-, imaging- és
fájltörlési hibák WARN/ERROR naplóbejegyzései a sikeres negatív tesztek részei.

## Böngészős regresszió

A `node tests/acceptance.cjs` végső eredménye **sikeres, kilépési kód 0**.
A futás hét ellenőrzési csoportja mind OK:

1. Több valódi JPG, feltöltés előtti kivétel, eredeti bájtok, borítóváltás és lista.
2. Egyedi művelet, kikapcsolás, költöztetési cél és végrehajtás.
3. Eladás rögzítése dialógusból, helyszínszűrés, SOLD rendezés.
4. Áthelyezés és teljes Windowsos szolgáltatás-/adatbázis-újraindítás utáni adatok.
5. Lomtár, végleges törlés megszakítása, újraindítás, visszaállítás, fotóhivatkozások.
6. Kétablakos 409, piszkozat és fotó megőrzése, navigációs és lapbezárási figyelmeztetés.
7. Hibás JPG 422, megszakított kérés, újrapróbálás, végleges DB- és fájltörlés.

A futás naplója `.local/jpa-acceptance.log`, az indulásé
`.local/jpa-acceptance-start.log`. A próba után a külön acceptance szolgáltatások
is leálltak a `scripts/Start.ps1 -TestRun -Stop` paranccsal; a tesztprofil adatai
megmaradtak. A személyes profil nem lett elindítva vagy leállítva.

Az eredeti próba a már létező eladási dialógus miatt a lap legelső űrlapját
tévesen helyszínűrlapnak tekintette. A teszt egyetlen választója a valódi
`#places form` területre lett szűkítve; a termék felülete változatlan.

## Személyes indítás a tesztek után

A felhasználó kérésére a személyes szerver is elindult. Ezt előbb tranzakciós
MySQL-mentés és fotómásolat előzte meg, a dump befejezési jelzésének, valamint a
fájlok SHA-256 értékeinek ellenőrzésével. A helyi, Gitből kizárt mentés:
`.local/backups/personal-before-jpa-a1e22a1c0daf48f49b058f0ff49a8e63/`, ellenőrző
listája `sha256-manifest.json`. Külön visszaállítási próba nem történt.
Az Inventory és Imaging health végpontja UP, a `/leltar` HTTP 200 választ adott.
A regressziós tesztek továbbra sem futottak a személyes adatbázison.

## Megváltozott fájlok

| Terület | Fájlok |
|---|---|
| Függőségek / konfiguráció | `pom.xml`, `inventory/pom.xml`, `inventory/src/main/resources/application.properties` |
| Meglévő szolgáltatások | `inventory/src/main/java/hu/boros/inventory/{Ledger,Actions,Photos,Web}.java` |
| Új entitások | `inventory/src/main/java/hu/boros/inventory/{LocationEntity,ActionEntity,ItemEntity,PhotoEntity,FileCleanupEntity}.java` |
| Új repositoryk | `inventory/src/main/java/hu/boros/inventory/{LocationRepository,ActionRepository,ItemRepository,PhotoRepository,FileCleanupRepository}.java` |
| DTO / mapper | `inventory/src/main/java/hu/boros/inventory/InventoryViews.java` |
| Tesztek | `inventory/src/test/java/hu/boros/inventory/PersistenceIT.java`, `scripts/Test-Persistence.ps1`, `tests/acceptance.cjs` |
| Dokumentáció | `README.md`, `docs/jpa-atallas.md`, `docs/jpa-teszteredmenyek.md` |

Nincs új migráció. A két meglévő migráció Git-diffje üres, helyi SHA-256:

```text
V1__inventory.sql     492B94DE998C7C9177996C41E3EB36A7B9F7F3AC82A66582E3774FC0F4F76264
V2__item_actions.sql  DAAC1D7A584285931CB12A3817C5847319126E5686FE4C053B437DD317EAD3D6
```

Változatlan: `imaging` modul, statikus UI, Thymeleaf sablonok, Dockerfile,
Compose, Windows indító/build scriptek, PWA-promptok és roadmap. A munka elején
már meglévő, követetlen `projektprompt.txt` nem lett módosítva, és nem része az
átállás commitjának. A GitHub-feltöltés külön lépés.

## Korlátok és nyitott kérdések

A lefuttatott ellenőrzésekben **nem maradt ismert funkcionális hiba**. A
`git diff --check` sikeres; az üzleti Java-forrásban nincs `JdbcTemplate`, a
védett, változatlan területek Git-diffje üres. A módosítás helyi, nem publikált.

- Docker és Docker CLI nincs telepítve ezen a gépen; tényleges konténerbuild és
  Compose-indítás nem volt futtatható. A Java 21 Maven-csomagolás sikeres,
  a Docker/Compose konfiguráció változatlan.
- Flyway a MySQL 9.7-ről figyelmeztetést ad: a támogatási ellenőrzése legutóbb 9.4-re
  hitelesített. A meglévő függőségverzió megmaradt, a V1/V2 migrációk sikerrel lefutottak.
- Személyes/éles adatbázison nem futott regressziós próba. A későbbi személyes
  indítás a felhasználó kérésére, a fent dokumentált mentés után történt.
- Valódi iPhone- vagy PWA-teszt nem történt; ez a 00A perzisztenciafázis.
- Az adatbázis és a fájlrendszer nem közös tranzakció. A folyamat váratlan kilövése
  alatti kompenzálás korlátja megmarad; részletek az architektúraleírásban.
