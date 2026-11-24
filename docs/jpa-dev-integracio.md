# JPA-átállás összeillesztése a dev ággal

**Dátum:** 2026-11-17. Ez a riport a JPA-feltöltéshez tartozó összeillesztést
dokumentálja, nem új PWA-fejlesztést vagy felhasználói tesztkiadást.

## Források és változtatások

- Korábbi, külön ellenőrzött JPA-commit: `4ab60617c225714ba9cfdd7a19aa5d6fc0e0caea`.
- A megőrzött dev alapja: `75e90c11e2d253271032728efe031ccc8421b878`, benne a korábbi
  PWA, mobilos fotókezelés és hozzáférési szűrő.
- Az aktuális munkarend és promptok ellenőrzött main forrása:
  `fc5f897c9d45ae1da99a4c904a230ce82d80c500`. A teljes main ág nem került a dev-be.
- A JPA-commit szabályos Git-merge útján került a dev történetébe. Az eredeti
  JPA-commit és a párhuzamos dev-fejlesztések megmaradnak; nincs force push.
- Az új `PersistenceIT` író kérései megkapták a dev-ben már korábban kötelező
  `X-Otthontar-Request: 1` fejlécet. A hozzáférési szűrő és a PWA működése nem lett
  átírva; a hibás fejlécek tiltását a meglévő `LocalAccessFilterTest` is ellenőrzi.
- A Windowsos acceptance hibás JPEG-mintája csonka JPEG-fejlécet kapott, hogy a
  meglévő böngészős előfeldolgozás után is a valódi imaging 422-es válaszát és az
  adatbázis-rollbacket ellenőrizze. A nem képként felismerhető korábbi `bad` mintát
  az újabb dev kliens már feltöltés előtt visszautasította.
- A Flyway V1/V2 változatlan. Személyes adat, fénykép, mentés, `.local/` és
  buildtermék nem része a commitnak. A korábbi `projektprompt.txt` kimaradt.

Az ellenőrzött kódot az e riportot tartalmazó merge-commit rögzíti; a közzétett
pontos dev commitra a main központi projektállapota hivatkozik.

## Újra lefuttatott ellenőrzések

| Ellenőrzés | Eredmény | Helyi bizonyíték |
|---|---|---|
| `scripts/Test-Persistence.ps1`, Maven `-Pmysql-it verify`, JDK 25 / Java 21 célszint | Sikeres; 10 egységteszt + 14 MySQL-integráció, 0 hiba, 0 kihagyott; mindkét modul csomagolva, Spotless sikeres | `.local/jpa-dev-integration.log` |
| Windowsos `tests/acceptance.cjs`, valódi MySQL + imaging | Sikeres, kilépési kód 0; mind a 7 ellenőrzési csoport OK, szolgáltatás- és adatbázis-újraindításokkal | `.local/jpa-dev-acceptance.log` |
| `tests/pwa.cjs`, saját mock HTTP-kiszolgáló + Edge | 2 ellenőrzési csoport sikeres; az offline deep-link csoport sikertelen | `.local/jpa-dev-pwa.log` |

A Java-tesztek bontása: `LedgerTest` 3, `LocalAccessFilterTest` 3,
`JpegOrientationTest` 2, `MobilePhotoTest` 2, `PersistenceIT` 14.
A MySQL-integráció saját, friss `inventory_jpa_test` sémát és külön MySQL-folyamatot
használt: `target/jpa-it/0ea0a0b5953949e78b6eabacee2b37cb/`. A futtató leállította
a saját adatbázisát. A személyes szervert nem indította újra.
A Windowsos acceptance külön `.local/acceptance` profilt használt; a próbák után
az ahhoz tartozó szolgáltatások is leálltak. Az eredeti bájtok, borító, egyedi
műveletek, eladás, költöztetés, 409-es ütközés, lomtár/restore/purge, hibás JPEG,
újrapróbálás és tényleges képfájltörlés ellenőrzése sikeres.

## Nyitott PWA-ellenőrzés

A `tests/pwa.cjs:129` próbában 30 másodperc után sem jelent meg az
„A szerver nem érhető el” címsor offline deep-link navigációkor. A korábbi mobilos
fotó-, JPEG-megőrzési, HEIC-hiba-, feltöltési korlát-, piszkozatmegőrzési,
online mentési és elrendezési próbák sikeresek.

Ez a teszt saját mock HTTP-kiszolgálót használ, nem hívja a JPA backendet. A
PWA-források, sablonok és `tests/pwa.cjs` Git-diffje a fenti dev alaphoz képest üres.
A hiba oka nem lett igazolva vagy javítva ebben a JPA-feltöltésben; nem állítjuk,
hogy a teljes PWA-regresszió sikeres. Külön kivizsgálás szükséges.

## Átadási határ

Ez fejlesztési mentés a `dev` ágra. A `teszt` ág és a kiadási címkék változatlanok;
nem készült új felhasználói tesztkiadás, és felhasználói elfogadás nincs igazolva.
Docker futtatókörnyezet és valódi iPhone-próba továbbra sem állt rendelkezésre.
Az eredeti, külön JPA-fázis 2026-11-16-i ellenőrzéseit a
[korábbi jegyzőkönyv](jpa-teszteredmenyek.md) tartalmazza; azok nem helyettesítik
az itt rögzített, összeillesztés utáni eredményeket.
