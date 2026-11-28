# Projektállapot – Otthontár

**Frissítve:** 2026-11-28. Ez a `main` ágon karbantartott állapotfelvétel, nem élő szervermonitor és nem a `main` kódjának funkciólistája.


## Otthontár 01G fejlesztése – 2026-11-28

A dátumozott leltári eladások, a végrehajtott költöztetések, az automatikus és kézi tervkapcsolás, valamint a jóváhagyott helyváltoztatás elérhető a [`dev` ágon](https://github.com/borosdavid/borosCode/commit/5822b97b761b110ceb1bbb471ce5e26e04c8066c). A csoport a **Megtörtént** művelettel zárható le. 23 Java-egységteszt, 27 MySQL 8.4.8 integrációs teszt, a Chromium-regresszió asztali és mobil nézetben a gyökérúton és az `/otthontar` alatt, valamint a csomagolt alkalmazás HTTP-próbája sikeres. A V6 új adatait külön MySQL-sémába visszaállítottuk, és összehasonlítottuk az eredeti leltári és naptári adatokkal. [Kézi próba és a 02-es fázis adatszerződése](https://github.com/borosdavid/borosCode/blob/5822b97b761b110ceb1bbb471ce5e26e04c8066c/docs/naptar-01g.md).

A címkézett `teszt`-kiadáshoz hátravan a védett HTTPS-tesztpéldány, majd a felhasználói próba asztalon és valódi iPhone-on. Az Ubuntu időzített mentését és fotó-visszaállítását éles sémaváltás előtt igazolni kell.

## 01F naptárfelület fejlesztése – 2026-11-27

Elkészült a havi naptár, az időrendi lista és a többtárgyas eseményszerkesztő. Az esemény tárgyadatlapról is indítható. A mentési hibák és az ütközések megőrzik a kitöltött űrlapot. Második nyitott eladási tervnél a meglévő terv linkje látszik. Kód a `dev` ágon ([`a6de08e`](https://github.com/borosdavid/borosCode/commit/a6de08e97ca75c13880deb5aee081404021b44a0)). Chromiumban sikeres a mobil- és asztali naptárteszt a gyökérútvonalon és `/otthontar` alatt, valamint a leltár/PWA regressziós csomag. 23 Java-egységteszt és 23 MySQL 8.4.8 integrációs teszt sikeres. A csomagolt alkalmazás `/otthontar` előtaggal is kiszolgálja a verziózott naptárfájlokat. A leltárműveletek párosítását a fenti 01G állapot követi. Az éles frissítés és a valódi iPhone Safari-próba hátravan. [Kézi próba asztalon és iPhone-on](https://github.com/borosdavid/borosCode/blob/a6de08e97ca75c13880deb5aee081404021b44a0/docs/naptar-01f.md)

## 01E naptár adatmodell és API – 2026-11-27

Elkészült a V5 Flyway-migráció, a hitelesített `/api/calendar`, a napokra szűrhető tervezett és tényleges időszak, valamint a tárgyankénti kiindulási hely. A mentés verziófeltételes. Az átfedő tervek figyelmeztetést adnak, tárgyanként egyszerre egy eladási terv lehet nyitva. Forrás a `dev` ágon [`5a03348`](https://github.com/borosdavid/borosCode/commit/5a0334872ebad9b36a4e2bf3c3c48efdab1c1ab6). 23 Java-egységteszt és 23 valódi MySQL 8.4.8 integrációs teszt sikeres, közülük 7 naptári teszt. A V5 migrációt meglévő V4-es sémán adatokkal, valamint üres sémán is ellenőriztük. Az első fiók, a HTTP-fotófeltöltés és az újraindítási próba az `/otthontar` előtaggal sikeres. A Chromium PWA-regresszió sikeres a gyökéren és az `/otthontar` alatt. A 01F erre az API-ra épül. A leltárműveletek kapcsolása és a teljes felhasználói átadás a 01G része. [API-szerződés](https://github.com/borosdavid/borosCode/blob/5a0334872ebad9b36a4e2bf3c3c48efdab1c1ab6/docs/naptar-api-01e.md)

## 01D-ig ellenőrzött ágfrissítés – 2026-11-27

A 00A és 01–01D fejlesztései, a 01C két korábbi javítása és a tárgyadatlap új képigazítása a `teszt` ([`051fc7f`](https://github.com/borosdavid/borosCode/commit/051fc7fd1146e6cbddc5a6fe5bb257ee8301b9e5)) és a `main` ([`6964fb8`](https://github.com/borosdavid/borosCode/commit/6964fb82a4eeead95fba38e0b29e55be5cf89b40)) ágon van. Új képjavítás a `dev` ágon ([`760bd81`](https://github.com/borosdavid/borosCode/commit/760bd81b95568867ade5862662910e7f2bd93991)): a mentett és feltöltésre kiválasztott képek középre igazítva, teljes egészében a keretbe férnek. A PWA gyorsítótára v10.

A Chromium PWA-teszt sikeres a gyökéren és az `/otthontar` útvonalon. Sikeres a Maven build, 23 Java-egységteszt, 16 MySQL 8.4.8 integrációs teszt, az első fiók és fotófeltöltés csomagolt JAR-próbája, 77 PowerShell-ellenőrzés Linuxon és az Ubuntu mentési fixture. [Tesztpróba](https://github.com/borosdavid/borosCode/blob/051fc7fd1146e6cbddc5a6fe5bb257ee8301b9e5/docs/tesztkiadasok/teszt-2026.11.27.1.md)

Éles szervertelepítés, valódi iPhone Safari-próba, élő Windows/Tailscale próba, illetve az Ubuntu automatikus mentése és fotó-visszaállítása továbbra is igazolásra vár. A `teszt-2026.11.27.1` annotált címke helyben létrejött, távoli közzétételéhez a GitHub-csatlakozó nem kínál tagműveletet és a Git kliensnek nincs hitelesítése.

## PWA telepítési útmutató – 2026-11-27

A „Telepítés iPhone-ra” blokk csak az iPhone Safari böngészőlapján jelenik meg. A főképernyőről önálló módban indított appban, asztali böngészőben és más iPhone-böngészőben rejtve marad. Megvalósítás a `dev` ágon ([`1db81eb`](https://github.com/borosdavid/borosCode/commit/1db81ebb57e8f9d3076586e630dbb3c713aa50a3)). A Chromium PWA-teszt sikeres a gyökér és az `/otthontar` útvonalon, iPhone Safari, önálló mód, iPhone Chrome és asztali Safari szimulált profiljaival. A PWA gyorsítótár v9-re lépett. Az éles telepítés és a valódi iPhone-próba hátravan.

## 01C borítóképek – 2026-11-27

A javítás a `dev` ágon van ([`ddba180`](https://github.com/borosdavid/borosCode/commit/ddba180dde71bcabb2ad5badbec111597aaad814)). A tárgykártyák borítóképe a Fotónézetben és a Listában középre igazítva, az eredeti képarányt megtartva, teljes egészében kitölti a rendelkezésre álló helyet az egyik tengely mentén. A keret szabadon maradó része szimmetrikus. A PWA gyorsítótár v8-ra lépett. A Chromium PWA-teszt 390 és 1440 px szélességen, álló és fekvő képekkel, a gyökér és az `/otthontar` útvonalon sikeres. Az Ubuntu-telepítés és a valódi iPhone Safari-próba hátravan.

## 01D napi Ubuntu-mentés – 2026-11-24

**Állapot: Fejlesztés alatt, az Ubuntu-mentés aktiválva.** A helyi előellenőrzés, az első éles titkosított archívum visszafejtése és SHA-256 ellenőrzése, valamint az elkülönített, hálózat nélküli MySQL-visszaállítás sikeres. Mind a 8 tábla és a táblánkénti rekordszám egyezett. A fotómappa üres volt. A napi systemd timer aktív, a linger engedélyezve van. Az első automatikus futás ellenőrzése és a valódi fotófájl visszaállítási próbája hátravan. [Üzembe helyezési napló](https://github.com/borosdavid/borosCode/blob/694a1701787b11188b38a4e298b2d6eccc8d19ed/docs/KNOWHOW-ubuntu-napi-mentes.md).

A napi mentés egy archívumfájlt készít futásonként, a fotók és DB konzisztenciája miatt az Inventory szolgáltatást a pillanatkép elkészültéig leállítja. A csak ugyanazon szerveren tartott mentés nem véd gépvesztés ellen; külső titkosított másodpéldány külön további feladat.

## Most hol tartunk?

- A felhasználó 2026-11-24-i visszajelzése szerint az Otthontár weboldala megfelelően működik. A kanonikus cím `https://boroscode.com/otthontar/`, az Ubuntu-példány Cloudflare Tunnelen át érhető el. A mobilos szerveradminisztráció útvonala iSH és SSH a Tailscale hálózatán. A Cloudflare-en végzett saját fiókos belépés, fotófeltöltés és iPhone PWA-átvétel dokumentált próbája még hátravan. [Hozzáférés](SZERVER-TAVOLI-HOZZAFERES.md) · [Cloudflare-terv](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/roadmap/CLOUDFLARE-BOROSCODE-DOMAIN.md)
- Elfogadott alkalmazáskövetelmény (D-008): felhasználónév/jelszó, azonos teljes alkalmazás-hozzáférés minden aktív fióknak. A belépés a dev ágon elkészült: [`815dcb2`](https://github.com/borosdavid/borosCode/commit/815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c); a valódi MySQL-integráció sikeres. A Cloudflare-es iPhone-átadás és a külön Windows-környezeti próba hátravan.
- D-011: a Windows fejlesztőgéphez elkülönített tesztszerver készült. A 01B Windows-segédprogramok és privát GitHub-ügynök a dev ágon elkészültek ([cf19142](https://github.com/borosdavid/borosCode/commit/cf191426810ded4ef4b362d56c35735165321cb4)). A javított dev-verzió [`f3c2f74`](https://github.com/borosdavid/borosCode/commit/f3c2f742fd57b4633a226f880d74685984549d1c) 77 PowerShell-ellenőrzésen ment át. Élő Windows/Tailscale/vezérlési kapcsolat még nincs igazolva.

- A PWA-alap a `dev` ágon van. A november 24-i [CI-futás](https://github.com/borosdavid/borosCode/actions/runs/45331467488) Java/MySQL és PowerShell feladata sikeres volt. Az Edge és a Chromium böngészőteszt a tárgylista betöltésénél leállt.
- A teljes offline leltár, a tartós offline szerkesztés és a kétirányú szinkron még további fejlesztés.
- A JPA-átállás a `dev` ágon igazolt és feltöltött: [`dae1224627a983217296ffe2dd47a339caacdb59`](https://github.com/borosdavid/borosCode/commit/dae1224627a983217296ffe2dd47a339caacdb59). Az újrafuttatott Maven/24 Java-teszt és a 7 Windowsos acceptance csoport sikeres; a külön PWA offline deep-link próba Edge alatt sikertelen, kivizsgálása nyitott.
- A `teszt` ágra emelt alkalmazáskód a 01D-ig ellenőrzött GitHub-commitban érhető el ([`051fc7f`](https://github.com/borosdavid/borosCode/commit/051fc7fd1146e6cbddc5a6fe5bb257ee8301b9e5)). A címkézett átadás és az eszközös próbák még nyitottak.
- A JPA-megvalósítás és az újrafuttatott alkalmazástesztek a dev-hez tartoznak. A main ezen frissítése csak dokumentáció és státusz; nem emeli át az alkalmazáskódot vagy a dev-et a teszt ágra.

## Következő teendő és akadályok

1. A [01C webes felület](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/01C-WEB-FELULET.md) esetén a `dev` implementáció elkészült, és a publikus belépőn az átdolgozott HTML már megfigyelhető; a Worker verziózott CSS/JS/manifest útvonalainak 404-es hibáját javítani, majd az Ubuntu teljes futó verzióját és a hitelesített működést ellenőrizni kell. Csak ezután jöhet a címkézett tesztátadás. [Kézi próbamenet](https://github.com/borosdavid/borosCode/blob/a2982a4e90baea4194db17bd8eb0c88fb8613f7d/docs/web-felulet-01c.md).
2. Az 01 és 01A meglévő PWA- és hozzáférésvédelmi ellenőrzései a kanonikus Cloudflare-es címen. Az iPhone-os offline használat előfeltétele az igazolt telepítés és belépés.
3. A következő új fejlesztés a [01E–01G költözési és eladási naptár](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/roadmap/KOLTOZESI-NAPTAR.md#ütemezés), online használatra asztalon és iPhone-on. A 01D automatikus mentésének és egy fotó visszaállításának ellenőrzése az éles naptármigráció előtt esedékes.
4. A naptár ellenőrzött átadása után jön a teljes offline olvasás (02), az offline írás és szinkron (03), majd a végső iPhone-átvétel (04).

A 00A Docker-próbája és a 01B Windows/privát GitHub-vezérlés élő átvétele külön nyitott ellenőrzés.

**Hosszú távú gépszerepek (D-007; D-011 szerint későbbre halasztva):** a Debian 13/Xfce laptop kizárólag szerver; tervezett alapja Docker Engine/Compose, Tailscale/SSH és privát HTTPS. A fejlesztés Windowsról és iPhone-ról történik. A Windows helyi fejlesztést/buildet/tesztet szolgál; az iPhone fejlesztések kezdeményezését és követését, valamint PWA-próbákat. Az iPhone-ról indított munka tényleges végrehajtási helyét és a telepítési csatornát még igazolni kell.

## Végrehajtási feladatok

| Feladat | Státusz | Igazolt eredmény / következő feltétel |
|---|---|---|
| [00A – JPA](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/00A-HIBERNATE-JPA-ATALLAS.md) | Fejlesztés alatt | Kód elkészült és dev-be feltöltve; 24 Java-teszt és 7 Windowsos csoport sikeres; külön tesztátadás/elfogadás még nincs |
| [01 – PWA és HTTPS](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/01-PWA-HTTPS-MOBIL-ALAP.md) | Fejlesztés alatt | Cloudflare-en elérhető a belépőoldal. A böngészős regresszió ellenőrzése és a valódi iPhone-átadás hátravan |
| [01A – alkalmazásfiókok](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/01A-FELHASZNALOI-FIOKOK.md) | Fejlesztés alatt | Fiókok/session, CSRF és fiókváltási védelem dev-be feltöltve; 18 egységteszt, 16 valódi MySQL-integrációs teszt és 4 böngészős csoport sikeres; a Cloudflare-es iPhone-átvétel és a külön Windows/Docker próba hátravan |
| [01B – Windows-tesztszerver és vezérlés](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md) | Fejlesztés alatt | Kód dev f3c2f74; 77 PowerShell-ellenőrzés sikeres; valós Windows/HTTPS/privát repo és innen működő vezérlés még hiányzik |
| [01C – letisztult webes felület](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/01C-WEB-FELULET.md) | Fejlesztés alatt | Új belépő, leltár-összesítő és váltható nézetek a [`dev` ágon](https://github.com/borosdavid/borosCode/commit/a2982a4e90baea4194db17bd8eb0c88fb8613f7d). Helyi Chromium-próba és [CI](https://github.com/borosdavid/borosCode/actions/runs/45352285164). A futó Ubuntu build teljes azonosítása, a verziózott CSS/JS hibák javítása és az eszközös átvétel hátravan |
| [01E–01G – költözési és eladási naptár](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/roadmap/KOLTOZESI-NAPTAR.md#ütemezés) | Fejlesztés alatt | Nap pontosságú események, eseményfelvitel tárgyadatlapról és kézi lezárás megerősítve. Többnapos események, az egyértelmű eladás automatikus párosítása és a lejárt terv nyitva tartása megerősítve. A tényleges kezdő- és végdátum, az átfedési figyelmeztetés, az egy nyitott eladási terv és a jóváhagyott helyfrissítés megerősítve. A 01E API és a V5 migráció elkészült a dev ágon ([`5a03348`](https://github.com/borosdavid/borosCode/commit/5a0334872ebad9b36a4e2bf3c3c48efdab1c1ab6)). [API-szerződés](https://github.com/borosdavid/borosCode/blob/5a0334872ebad9b36a4e2bf3c3c48efdab1c1ab6/docs/naptar-api-01e.md) A 01F felület elkészült ([`a6de08e`](https://github.com/borosdavid/borosCode/commit/a6de08e97ca75c13880deb5aee081404021b44a0)), a 01G leltárintegráció következik. A naptári Eladva műveletről az első próba után születik döntés. Megvalósítás és átadás hátravan. [Naptárterv](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/roadmap/KOLTOZESI-NAPTAR.md#megerősített-működés) |
| [02 – offline leltár](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/02-TELJES-OFFLINE-LELTAR-ELOZETEK.md) | Tervezett | A 01C felületére, az elfogadott online naptárra és a védett iPhone PWA-elérésre épül |
| [03 – offline szerkesztés/szinkron](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/03-OFFLINE-MODOSITAS-KAMERA-SZINKRON.md) | Tervezett | A 02-es konzisztens olvasási/változásnapló-alapjára épül |
| [04 – iPhone és átadás](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/prompts/04-IPHONE-TESZT-ADATVEDELEM-ATADAS.md) | Tervezett | Előző fázisok és elérhető eszközös tesztkörnyezet szükséges |

A tágabb tervek állapotát a [roadmap](https://github.com/borosdavid/borosCode-knowledge/blob/main/projects/otthontar/roadmap/README.md) kapcsolt adatlapjai rögzítik. A részletes cél, az ötlet/vizsgálati pontosítás és az előfeltételek a saját adatlapjukban vannak.

## Ellenőrzött források és korlátok

A dokumentációs feladat kezdetén az alábbi távoli állapotokat olvastuk. Ezek rögzített ellenőrzési pontok; az utána készült munkarendi commitok miatt a branch feje továbbmozdulhat.

| Forrás | Rögzített állapot | Mit bizonyít? |
|---|---|---|
| dev | [`6824ba30bcaa634cc8d96bef134d32774c4f5e75`](https://github.com/borosdavid/borosCode/commit/6824ba30bcaa634cc8d96bef134d32774c4f5e75) | A helyi PWA-kód és riport jelen van |
| main (történeti pillanatkép) | [`040dad1f0f1f24ab33d824257e8b3e9a03a3f8b7`](https://github.com/borosdavid/borosCode/commit/040dad1f0f1f24ab33d824257e8b3e9a03a3f8b7) | Az átállás előtti tervek és munkarend; az aktuális prompt/roadmap már a privát tudástárban van |
| teszt | [`75ab347af6813290047232d45454190c43992655`](https://github.com/borosdavid/borosCode/commit/75ab347af6813290047232d45454190c43992655) | Alapállapot; önmagában nem tesztkiadás |
| dev – JPA összeillesztés | [`dae1224627a983217296ffe2dd47a339caacdb59`](https://github.com/borosdavid/borosCode/commit/dae1224627a983217296ffe2dd47a339caacdb59) | A korábbi PWA és a JPA együtt; a Windowsos újrafuttatás bizonyítékai és nyitott PWA-próba a kód melletti riportban |

- [PWA-riport](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/docs/pwa-01.md): korábbi, 2026-11-17-i 10 sikeres Java-teszt és 3 sikeres böngészős tesztcsoport. Ebben a dokumentációs feladatban ezeket nem futtattuk újra.
- A korábbi [Ledger](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/inventory/src/main/java/hu/boros/inventory/Ledger.java) és [inventory/pom.xml](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/inventory/pom.xml) még JdbcTemplate-et használt. Ezt a történeti állapotot a fenti JPA-commit felváltja; [architektúra és önálló fázisriport](https://github.com/borosdavid/borosCode/blob/dae1224627a983217296ffe2dd47a339caacdb59/docs/jpa-atallas.md).
- A korábbi PWA-riport proxyforgalomra 403-at jelzett. A 01A ezt explicit natív HTTPS-origin és belépési védelemmel bővítette; a 01B a Windows/Tailscale beállítót adja hozzá. A valódi eszközös proxypróba még szükséges.
- A Windows teljes acceptance és a natív, izolált MySQL-integráció most újra lefutott: [dev integrációs riport](https://github.com/borosdavid/borosCode/blob/dae1224627a983217296ffe2dd47a339caacdb59/docs/jpa-dev-integracio.md). A külön PWA offline próba nem sikeres; Docker-futtatás, valódi iPhone-próba és Debian-szerverkapcsolat továbbra sem igazolt.

## Hol mit keresünk?

| Tartalom | Hiteles forrás |
|---|---|
| Promptok, roadmapek, közös tervezés | privát `borosCode-knowledge/main/projects/otthontar/` |
| Projektspecifikus státuszok, tesztbizonyítékok és projektirányítás | `borosCode/main/projekt/` |
| Folyamatban lévő kód és hozzá illő műszaki riport | dev |
| Átadott tesztkiadás és pontos tesztútmutató | teszt; a későbbi kiadás címkéje rögzíti az állapotot |
| Munkarend | AGENTS.md és docs/branch-workflow.md minden használt ágon |

## Karbantartás

Feladatkezdéskor és érdemi állapotváltozáskor az adatlapok után ezt az oldalt is frissítsd: dátum, következő feladat, akadályok, commitok és átadott verzió. Távoli hozzáférés nélkül ne állíts élő szerverállapotot vagy nem pusholt asztali eredményt.

Kapcsolódás: [státuszszabályok](STATUSZOK.md). Az oldal frissítése a fejlesztő feladata; nincs automatikus időzített háttérfigyelés.

Tesztkiadások: [verziózás és átadási eljárás](TESZTVERZIOK.md), [átadási sablon](sablonok/TESZTATADAS.md). 2026-11-17-én nem volt Git-tag vagy GitHub Release; új alkalmazás-tesztkiadást a mostani dokumentációs feladat nem hozott létre.

Döntések és nyitott javaslatok: [döntési napló](DONTESI-NAPLO.md). A D-006 gépszerepekre és alapösszeállításra vonatkozó részét D-007 feloldotta. A hardver, jogosultságok, mentés és tényleges vezérlési kapcsolat részletei továbbra is tisztázandók.

A jogosultsági és vezérlési pontosítások: [D-008/D-009 a naplóban](DONTESI-NAPLO.md), [távoli hozzáférési terv](SZERVER-TAVOLI-HOZZAFERES.md). A külön privát vezérlőrepó még nincs igazoltan létrehozva/csatlakoztatva; a 01B Windows-ügynök kódja dev-be került, de helyi telepítése/élő próbája hátravan. Az alkalmazásfiók nem szerveradminisztrációs fiók.

## Legutóbbi fejlesztés – 01A, 2026-11-17

A JPA/PWA `dae1224` kódjára épített fiókfejlesztés: [815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c](https://github.com/borosdavid/borosCode/commit/815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c). Felhasználónév/jelszó, egyenlő közös alkalmazásjog, BCrypt-hash, session/CSRF, fiókletiltás és jelszócsere utáni online session-visszavonás, újrabelépéskor megőrzött nyitott piszkozat/fotó, fiókváltás ellen védett írás. Első fiókot helyben kell létrehozni; nincs alapjelszó.

Maven verify és 18 Java-teszt sikeres; a PWA fixture 4 csoportja Headless Chromium 131 alatt sikeres. A korábban jelzett Windows/Edge offline hibát ez nem zárja le. A módosított MySQL/Windows harness lefordult, de a valódi MySQL V3-migráció/karbantartás, Windows helper és acceptance, Docker, Tailscale/HTTPS és iPhone még nem volt ellenőrizhető. A natív explicit HTTPS-proxykonfiguráció tesztelve van szimulált kérésekkel; a Docker távoli proxy-topológiája külön szervertelepítési feladat.

Részletes, kódhoz kötött riport és fióklétrehozás: [01A átadási leírás](https://github.com/borosdavid/borosCode/blob/815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c/docs/fiokok-01a.md). A `teszt` ág és a kiadási címkék változatlanok. Következő gyakorlati lépés a valódi izolált adatbázisos/eszközös ellenőrzés és az alábbi 01B Windows-kód helyi aktiválása; a 02/03 offline fázisok még nem indultak el.

## Legutóbbi fejlesztés – 01B Windows, 2026-11-17

A 01A `815dcb2` dev-kódjára épülő [cf191426810ded4ef4b362d56c35735165321cb4](https://github.com/borosdavid/borosCode/commit/cf191426810ded4ef4b362d56c35735165321cb4) elkészíti az átmeneti natív Windows-tesztszerver indítását/állapotát/leállítását/újraindítását, a build-commit/JAR-hashek ellenőrzését, a Tailscale Serve HTTPS-beállítót és a DPAPI-védett, privát GitHub-kéréseket kezelő helyi ügynököt. Az alkalmazás leállítása a MySQL-t, személyes profilt, fotókat és a vezérlőt megtartja; a Stop-Process nem igazolt kulturált JVM-leállítás. A diagnostics jelenleg szűrt health/verzióösszesítés, nem nyersnapló-hozzáférés. A vezérlő kézzel indított előtérfolyamat, nem automatikus szolgáltatás.

71 sikeres assertion PowerShell 7.5.13/Linux alatt: parser, séma/cél/lejárat, lock/napló/replay/crash/timeout, eredményszűrés, valódi Git/buildjegyzék és módosított JAR/forrás, mock GitHub/HTTPS/tesztfolyamatok. Ez nem Windows-eszközteszt. Java-kód és migráció ebben a fázisban nem változott; Maven/01A teszteket most nem futtattuk újra.

[Helyi aktiválási parancsok és a hiányzó élő próbák](https://github.com/borosdavid/borosCode/blob/cf191426810ded4ef4b362d56c35735165321cb4/docs/windows-tesztszerver-01b.md). A Windows bekapcsolt/ébren lévő állapota és az elindított ügynök szükséges az online vezérléshez. A privát repo létrehozását/csatlakoztatását, helyi tokent és Tailscale-bejelentkezést ez a felhős munkamenet nem helyettesíti. A 01B aktív marad; nincs új tesztágas kiadás vagy címke. A D-011 döntés azonnali Debian-telepítés helyett átmeneti Windows-tesztet rögzít; az offline 02/03 továbbra is következő külön fejlesztés.

## A 2026-11-18-i projektátvizsgálás javításai

A dev [`f3c2f74`](https://github.com/borosdavid/borosCode/commit/f3c2f742fd57b4633a226f880d74685984549d1c) öt javítást tartalmaz. A tesztszerver újraindítása leállítás előtt ellenőrzi a buildet és a HTTPS-beállítást. Az átírt vezérlési kérés után a sor továbbhalad. A Windowsos böngészős teszt lapjai közös belépést használnak, és az első fiók létrehozása bekerült az indítási útmutatóba. A fotós mentés elveszett válasz után ugyanazzal a műveletazonosítóval biztonságosan újrapróbálható.

Az elkülönített MySQL 8.4.8 integráció 16 tesztje sikeres, beleértve a V2→V4 migrációt meglévő tesztadatokkal és a párhuzamos ismétlést. További 18 Java-egységteszt, 4 Chromium-tesztcsoport és 77 PowerShell-ellenőrzés sikeres. A Windowsos teljes próba, interaktív fiókkarbantartás, iPhone HTTPS és az élő privát vezérlés a gépeken ellenőrizendő. A fázisok státusza továbbra is Fejlesztés alatt.

## Automatikus tesztelés és első fiók, 2026-11-18

A [dev munkafolyamat](https://github.com/borosdavid/borosCode/commit/b44f8bd7ed91ec25af7079e77106222df246911d) a pull requesteknél és a projektágakra történő frissítéskor futtatja a Java/MySQL teszteket, a Chromium és Edge PWA-próbát, valamint a Windows PowerShell 5.1 ellenőrzést. Az első futás PowerShell 5.1 alatt kódolási hibát talált a fiókkezelő szkriptben. A javítás az azonos kódolású három Windows-szkriptet érinti. A [kódolási javítás](https://github.com/borosdavid/borosCode/commit/16ad22ba72ed86d0a806f5f6a58d2b851fdf2af3) után a Windows-ellenőrzés sikeres. [GitHub futás](https://github.com/borosdavid/borosCode/actions/runs/44285364279).

A [Linux/MySQL alkalmazáspróba](https://github.com/borosdavid/borosCode/commit/be040d584e1a26a10bfbc933ed5a2e53c43106db) üres tesztsémán, interaktív terminálon hozza létre az első fiókot a becsomagolt JAR-ral. Ellenőrzi a belépést, a valódi képszolgáltatással készülő előnézetet és az eredeti fotót a leltárszolgáltatás újraindítása után. A Windowsos szolgáltatásvezérlés, a saját iPhone-on működő Tailscale HTTPS és az élő privát távoli állapotlekérés még eszközpróbát igényel. [Ellenőrzőlista](https://github.com/borosdavid/borosCode/blob/16ad22ba72ed86d0a806f5f6a58d2b851fdf2af3/docs/windows-tesztszerver-01b.md#ellenőrzések-és-hátralévő-próbák).

A státuszfrissítéshez használt main kiindulási pont: `d53d8a8`.
