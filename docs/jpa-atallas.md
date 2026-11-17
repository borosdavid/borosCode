# Hibernate/JPA átállás – 00A

## Kiinduló audit

Alap: `ce9fdd60810f403c2f2145e032c50600209faa25` (`main`, 2026-11-16).
A helyi `projektprompt.txt` már a munka előtt követetlen fájl volt, nem része az átállásnak.

| Korábbi JDBC használat | Megőrzendő viselkedés |
|---|---|
| `Ledger.places/place` | Név szerinti helyszínlista, egyedi név, hiányzó átnevezés: 409. |
| `Ledger.items/item` | `trash` szerinti lista, SOLD hátul, név/id sorrend; a listában `place`, az adatlapon rendezett `photos`; snake_case adatmezők. |
| `Ledger.save` | Új verzió 1; feltételes frissítés +1, lomtárban nem menthető; művelet megosztott zárolása, inaktív művelet csak korábbi hozzárendelésnél; képek és borító egy tranzakcióban. |
| `Ledger.completeSale/transition` | Verzió- és állapotfeltételek, 409; MOVE végrehajtható kikapcsolt művelettel is; purge kizárólag lomtárból, írási zárolással és tartós képtörlési sorral. |
| `Actions.all/save` | KEEP/SALE/MOVE/CUSTOM sorrend, egyedi név, új MOVE/CUSTOM aktív; meglévő típus nem módosítható. |
| `Photos.cleanup`, `Web.photo` | Legfeljebb 100 tartós törlés újrapróbálása; fotó csak létező adatbázisrekord és fájl esetén érhető el. |
| `Photos.store` | Imaging HTTP + eredeti/előnézet fájl; rollback utáni kompenzáló törlés. A fájlrendszer nem adatbázis-tranzakció. |

Az igazodási alap az érintetlen Flyway V1/V2: `location`, `item`, `photo`,
`item_action`, `file_cleanup`. A UUID-k CHAR(36), az action azonosító VARCHAR(36),
az árak DECIMAL(14,2), a dátum DATE. A `photo.item_id` adatbázisban ON DELETE CASCADE.
Az `item.cover` ellenőrzött logikai hivatkozás, nincs hozzá adatbázisbeli idegen kulcs.

## Entitások és repositoryk

Az `inventory` Spring Boot által kezelt `spring-boot-starter-data-jpa` függőséget
használja: Boot 4.1.2, Spring Data JPA 4.1.2, Hibernate ORM 7.4.13.Final. Nincs külön
Hibernate-verzió rögzítve. A Java célszint továbbra is 21.

| Entitás / repository | Szerep és kapcsolat |
|---|---|
| `LocationEntity` / `LocationRepository` | Helyszín, rendezett lista, feltételes átnevezés. |
| `ActionEntity` / `ActionRepository` | Művelet, korábbi rendezés, beállítások, hozzárendeléskor PESSIMISTIC_READ zárolás. |
| `ItemEntity` / `ItemRepository` | Tárgy; LAZY helyszín/művelet/célhelyszín, verziófeltételes JPQL írások, listához helyszín fetch join. |
| `PhotoEntity` / `PhotoRepository` | LAZY tárgyhivatkozás; `position_no`, majd id szerinti fotósorrend. |
| `FileCleanupEntity` / `FileCleanupRepository` | Tartós törlési feladatok, legfeljebb 100 egy takarításban. |

Az írás az explicit idegenkulcs-azonosítómezőket használja, az azonos oszlopra
mutató LAZY kapcsolatok csak olvashatók (`insertable=false`, `updatable=false`).
Nincs cascade persist vagy orphanRemoval. A purge az eredeti adatbázisbeli
ON DELETE CASCADE-ot használja. A JPEG-fájlok formátuma és elérési útja változatlan.

A `Ledger`, `Actions`, `Photos` és a fotóvégpont saját üzleti adatelérése teljesen
JPA-alapú. Közvetlen JDBC kizárólag az integrációs tesztben marad a régi SQL-ekkel
végzett független összehasonlításhoz és tesztadat-előkészítéshez.

## DTO és külső szerződés

Az `InventoryViews` explicit `PlaceView`, `ItemView`, `PhotoView` rekordokat és
mappert tartalmaz. A REST réteg nem szerializál entitásokat. Az action válasza
továbbra is az `Actions.Choice`. A multipart bemenet továbbra is `Ledger.Draft`:
`location`, `actionId`, `soldDate`, `destination` stb.

A kimenetben megmaradnak a snake_case mezők, a null értékek, numerikus árak,
boolean jelölők és ISO dátumok. A listában `place` szerepel, `photos` nem; az
adatlapon ennek fordítottja. A kérésparaméter tényleges neve továbbra is `trash`.
A böngészős kód és a Thymeleaf sablonok változatlanok. Az Open EntityManager in
View kikapcsolt. A tárgylista egy fetch joinos lekérdezés, az adatlap kettő
(tárgy és fotólista); szerializációkor nincs további adatbázis-lekérdezés.

## Verziók, zárolás, tranzakciók

Az `item.version` szándékosan nem `@Version`: az induló 1, minden üzleti módosítás
pontos +1 számozása és az eredeti SQL állapotfeltételei explicit JPQL update-ekben
maradnak. A borító mentése nem növeli még egyszer a verziót. Az érintett sorok
száma dönti el a HTTP 409-et. A sale külön tranzakciót kapott a repository update
futtatásához. Az inaktív műveletek és a MOVE eredeti szabályai megmaradtak.

Az új, kliens által megadott UUID-jú tárgy `EntityManager.persist` műveletet használ,
nem merge-et: ismételt id nem írhatja felül a meglévő tárgyat. A persist utáni
repository flush az idegenkulcs- és egyediségi hibákat azonnal, a meglévő 409-es
hibakezelésen át jelzi. Minden fotórekord flush-olódik még a mentési tranzakcióban.

Két MySQL-specifikus native JPA query indokolt:

- Purge: `INSERT ... SELECT` a fotóazonosítókat egy tranzakcióban helyezi a törlési
  sorba, a fotók és a tárgy eltávolítása előtt.
- Sikertelen rollback utáni fájltörlés: idempotens `INSERT IGNORE`, külön
  `REQUIRES_NEW` tranzakcióban, mert a tárgy tranzakciója már visszagördült.

A rollback callback továbbra is eltávolítja az eredetit és az előnézetet. Ha ez
fájlhiba miatt nem sikerül, most tartós újrapróbálási feladat készül; a korábbi
kód ebben az esetben csak naplózott. A takarítás csak mindkét fájl sikeres törlése
után törli a feladatot. Hiányzó fájl újrapróbálása biztonságos.
Az adatbázis és fájlrendszer továbbra sem közös elosztott tranzakció: folyamatkilövés
pontos pillanatában vagy egyszerre fennálló fájl- és adatbázishibánál a callback
nem jelent teljes körű tartóssági garanciát.

## Flyway és adatmegőrzés

Nincs új migráció vagy sémaváltozás. V1/V2, indexek és idegen kulcsok érintetlenek.
Éles és tesztindításkor is `spring.jpa.hibernate.ddl-auto=validate`; sémát kizárólag
Flyway migrálhat. A teszt az indulás előtti/utáni SHOW CREATE TABLE-t is összeveti.

A személyes `.local/personal` profil, adatbázis, fotók és Docker-volumek nem voltak
tesztcélpontok. A tesztek utáni, felhasználó által kért személyes indítást ellenőrzött
MySQL- és fotómentés előzte meg; részletek a tesztjegyzőkönyvben. Éles adatokon
végzett további adatbázispróbákhoz továbbra is mentés és külön jóváhagyás szükséges.

## Reprodukálható ellenőrzés

Alap: `mvn verify`, ezen a gépen `powershell -ExecutionPolicy Bypass -File scripts/Build.ps1`.
Az eredeti formázó JDK 25 alatt `NoSuchMethodError` hibát adott új fájlokon; a
meglévő konfigurációban a google-java-format 1.35.0-ra frissült.

MySQL integráció: `powershell -ExecutionPolicy Bypass -File scripts/Test-Persistence.ps1`.
A futtató saját `target/jpa-it/<véletlen-azonosító>/mysql` adatkönyvtárat, véletlen
loopback portot és `inventory_jpa_test` sémát használ. Nem olvas személyes jelszót,
nem indítja a személyes profilt, nem töröl meglévő könyvtárat. A saját MySQL-t
`finally` ágban állítja le; a diagnosztikai naplók és adatok megmaradnak.
A `-MySqlBin` kapcsolóval más telepített MySQL Windows bináris megadható.

Más környezetben új, üres MySQL-séma használható:

```text
mvn -Pmysql-it verify -Dtest.db.url=jdbc:mysql://127.0.0.1:<port>/inventory_jpa_test -Dtest.db.user=<user>
```

A jelszó az `OTTHONTAR_TEST_DB_PASSWORD` környezeti változóból jön. Az IT elutasítja
a személyes `inventory` sémát, nincs Flyway clean. Dockerben kizárólag külön
tesztkonténer és tesztadat-kötet használható. A `mysql-it` profil Failsafe-val indítja
a `PersistenceIT` teszteket. Profil nélkül a `verify` egységteszteket, csomagolást
és formázásellenőrzést futtat; ekkor nincs adatbázis-integrációs próba.

A böngészős regresszió az eredeti `tests/acceptance.cjs`, kizárólag a külön
`.local/acceptance` profillal, 18740/18741 HTTP- és 43316 MySQL-porttal. Ez valódi
imaging szolgáltatást használ; a Maven IT célzott imaging HTTP-helyettesítővel
kényszeríti ki a feldolgozási hibákat. Eredmények a
[tesztjegyzőkönyvben](jpa-teszteredmenyek.md).

## Fázishatár és későbbi kézi ellenőrzés

A 01–04 PWA-fázis, hálózati elérés, hitelesítés és offline szinkron változatlan.
Valódi iPhone-teszt nem történt. Később, védett HTTPS-elérés és megfelelő jogosultság
kialakítása után kézzel ellenőrizendő: lista/adatlap, mentés, eladás, költöztetés,
lomtár/restore, kétablakos 409, kis/eredeti JPEG megnyitása.

Hivatkozások: [Spring Boot SQL/JPA](https://docs.spring.io/spring-boot/reference/data/sql.html),
[Spring Data zárolás](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html),
[Hibernate 7.4 kézikönyv](https://docs.hibernate.org/orm/7.4/userguide/html_single/),
[google-java-format 1.35.0](https://github.com/google/google-java-format/releases/tag/v1.35.0).
