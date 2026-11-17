# Projektállapot – Otthontár

**Frissítve:** 2026-11-17. Ez a `main` ágon karbantartott állapotfelvétel, nem élő szervermonitor és nem a `main` kódjának funkciólistája.

## Most hol tartunk?

- Elfogadott alkalmazáskövetelmény (D-008): felhasználónév/jelszó, azonos teljes alkalmazás-hozzáférés minden aktív fióknak. A belépés a dev ágon elkészült: [`815dcb2`](https://github.com/borosdavid/borosCode/commit/815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c); a valódi adatbázisos és eszközös átadás még hátravan.
- A közös távoli hozzáférés konkrét terve elkészült (D-009): felhasználói Tailscale/SSH, külön privát GitHub-műveletsor és korlátozott Debian-ügynök. Élő kapcsolat még nincs.

- A helyi PWA-alap a `dev` ágon van; a telefonról megnyitható, védett HTTPS-elérés még nincs igazolva.
- A teljes offline leltár, a tartós offline szerkesztés és a kétirányú szinkron még további fejlesztés.
- A JPA-átállás a `dev` ágon igazolt és feltöltött: [`dae1224627a983217296ffe2dd47a339caacdb59`](https://github.com/borosdavid/borosCode/commit/dae1224627a983217296ffe2dd47a339caacdb59). Az újrafuttatott Maven/24 Java-teszt és a 7 Windowsos acceptance csoport sikeres; a külön PWA offline deep-link próba Edge alatt sikertelen, kivizsgálása nyitott.
- A `teszt` ág jelenlegi alkalmazáskódja induló alapállapot, nem átadott PWA-tesztkiadás.
- A JPA-megvalósítás és az újrafuttatott alkalmazástesztek a dev-hez tartoznak. A main ezen frissítése csak dokumentáció és státusz; nem emeli át az alkalmazáskódot vagy a dev-et a teszt ágra.

## Következő teendő és akadályok

1. A korábban kijelölt 01B feladat előkészítése: a dedikált Debian-szerver hardverének/telepítésének felmérése és hitelesített kapcsolat kialakítása Windowsról, iPhone-ról és a használt ChatGPT-környezetből.
2. A JPA megvalósítása és dev-be mentése elkészült. Külön tesztágas átadás még nincs; a dev meglévő PWA offline hibájának kivizsgálása és a Docker-futtatás ellenőrzése hátravan.
3. Az 01-es fázis hozzáférésvédelmének és telefonos HTTPS-elérésének befejezése, majd tesztkiadás.
4. Ezután a teljes offline olvasás (02), offline írás/szinkron (03), végső iPhone-próbák (04).

**Elfogadott gépszerepek (D-007, 2026-11-17):** a Debian 13/Xfce laptop kizárólag szerver; tervezett alapja Docker Engine/Compose, Tailscale/SSH és privát HTTPS. A fejlesztés Windowsról és iPhone-ról történik. A Windows helyi fejlesztést/buildet/tesztet szolgál; az iPhone fejlesztések kezdeményezését és követését, valamint PWA-próbákat. Az iPhone-ról indított munka tényleges végrehajtási helyét és a telepítési csatornát még igazolni kell.

**Nyitva marad:** a laptop hardvere és kompatibilitása, a helyi első telepítés, jogosultságok, mentési célhely és a valóban használható távoli vezérlési kapcsolat. A kiválasztott összeállítás még nem kész telepítés. Tailscale vagy azonos ChatGPT-fiók önmagában nem ad ebből a beszélgetésből géphozzáférést.

## Végrehajtási feladatok

| Feladat | Státusz | Igazolt eredmény / következő feltétel |
|---|---|---|
| [00A – JPA](../prompts/00A-HIBERNATE-JPA-ATALLAS.md) | Fejlesztés alatt | Kód elkészült és dev-be feltöltve; 24 Java-teszt és 7 Windowsos csoport sikeres; külön tesztátadás/elfogadás még nincs |
| [01 – PWA és HTTPS](../prompts/01-PWA-HTTPS-MOBIL-ALAP.md) | Fejlesztés alatt | Helyi PWA kész; Windows/Edge offline deep-link próba sikertelen, hozzáférés és valódi iPhone-átadás hiányzik |
| [01A – alkalmazásfiókok](../prompts/01A-FELHASZNALOI-FIOKOK.md) | Fejlesztés alatt | Fiókok/session, CSRF és fiókváltási védelem dev-be feltöltve; 18 Java-teszt/4 böngészős csoport sikeres; valódi MySQL és HTTPS/iPhone-próbák hátravan |
| [01B – távoli vezérlés](../prompts/01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md) | Tervezett | Debian-szerver és Windows/iPhone fejlesztési szerepek eldöntve; telepítés és kapcsolat még nem igazolt |
| [02 – offline leltár](../prompts/02-TELJES-OFFLINE-LELTAR-ELOZETEK.md) | Tervezett | Előfeltétel a JPA-állapot és a védett elérés rendezése |
| [03 – offline szerkesztés/szinkron](../prompts/03-OFFLINE-MODOSITAS-KAMERA-SZINKRON.md) | Tervezett | A 02-es konzisztens olvasási/változásnapló-alapjára épül |
| [04 – iPhone és átadás](../prompts/04-IPHONE-TESZT-ADATVEDELEM-ATADAS.md) | Tervezett | Előző fázisok és elérhető eszközös tesztkörnyezet szükséges |

A tágabb [roadmap-tervek](../roadmap/README.md) jelenleg Tervezett állapotúak. A részletes cél, az ötlet/vizsgálati pontosítás és az előfeltételek a saját adatlapjukban vannak.

## Ellenőrzött források és korlátok

A dokumentációs feladat kezdetén az alábbi távoli állapotokat olvastuk. Ezek rögzített ellenőrzési pontok; az utána készült munkarendi commitok miatt a branch feje továbbmozdulhat.

| Forrás | Rögzített állapot | Mit bizonyít? |
|---|---|---|
| dev | [`6824ba30bcaa634cc8d96bef134d32774c4f5e75`](https://github.com/borosdavid/borosCode/commit/6824ba30bcaa634cc8d96bef134d32774c4f5e75) | A helyi PWA-kód és riport jelen van |
| main | [`040dad1f0f1f24ab33d824257e8b3e9a03a3f8b7`](https://github.com/borosdavid/borosCode/commit/040dad1f0f1f24ab33d824257e8b3e9a03a3f8b7) | Központi tervek és munkarend |
| teszt | [`75ab347af6813290047232d45454190c43992655`](https://github.com/borosdavid/borosCode/commit/75ab347af6813290047232d45454190c43992655) | Alapállapot; önmagában nem tesztkiadás |
| dev – JPA összeillesztés | [`dae1224627a983217296ffe2dd47a339caacdb59`](https://github.com/borosdavid/borosCode/commit/dae1224627a983217296ffe2dd47a339caacdb59) | A korábbi PWA és a JPA együtt; a Windowsos újrafuttatás bizonyítékai és nyitott PWA-próba a kód melletti riportban |

- [PWA-riport](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/docs/pwa-01.md): korábbi, 2026-11-17-i 10 sikeres Java-teszt és 3 sikeres böngészős tesztcsoport. Ebben a dokumentációs feladatban ezeket nem futtattuk újra.
- A korábbi [Ledger](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/inventory/src/main/java/hu/boros/inventory/Ledger.java) és [inventory/pom.xml](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/inventory/pom.xml) még JdbcTemplate-et használt. Ezt a történeti állapotot a fenti JPA-commit felváltja; [architektúra és önálló fázisriport](https://github.com/borosdavid/borosCode/blob/dae1224627a983217296ffe2dd47a339caacdb59/docs/jpa-atallas.md).
- A PWA-riport szerint a Tailscale/proxyforgalom jelenleg 403-at kap; a jelenlegi hozzáférési szűrő nem kész távoli hitelesítés.
- A Windows teljes acceptance és a natív, izolált MySQL-integráció most újra lefutott: [dev integrációs riport](https://github.com/borosdavid/borosCode/blob/dae1224627a983217296ffe2dd47a339caacdb59/docs/jpa-dev-integracio.md). A külön PWA offline próba nem sikeres; Docker-futtatás, valódi iPhone-próba és Debian-szerverkapcsolat továbbra sem igazolt.

## Hol mit keresünk?

| Tartalom | Hiteles forrás |
|---|---|
| Promptok, roadmapek, státuszok és projektirányítás | main |
| Folyamatban lévő kód és hozzá illő műszaki riport | dev |
| Átadott tesztkiadás és pontos tesztútmutató | teszt; a későbbi kiadás címkéje rögzíti az állapotot |
| Munkarend | AGENTS.md és docs/branch-workflow.md minden használt ágon |

## Karbantartás

Feladatkezdéskor és érdemi állapotváltozáskor az adatlapok után ezt az oldalt is frissítsd: dátum, következő feladat, akadályok, commitok és átadott verzió. Távoli hozzáférés nélkül ne állíts élő szerverállapotot vagy nem pusholt asztali eredményt.

Kapcsolódás: [státuszszabályok](STATUSZOK.md). Az oldal frissítése a fejlesztő feladata; nincs automatikus időzített háttérfigyelés.

Tesztkiadások: [verziózás és átadási eljárás](TESZTVERZIOK.md), [átadási sablon](sablonok/TESZTATADAS.md). 2026-11-17-én nem volt Git-tag vagy GitHub Release; új alkalmazás-tesztkiadást a mostani dokumentációs feladat nem hozott létre.

Döntések és nyitott javaslatok: [döntési napló](DONTESI-NAPLO.md). A D-006 gépszerepekre és alapösszeállításra vonatkozó részét D-007 feloldotta. A hardver, jogosultságok, mentés és tényleges vezérlési kapcsolat részletei továbbra is tisztázandók.

A jogosultsági és vezérlési pontosítások: [D-008/D-009 a naplóban](DONTESI-NAPLO.md), [távoli hozzáférési terv](SZERVER-TAVOLI-HOZZAFERES.md). A külön privát vezérlőrepó és az ügynök még nincs létrehozva/telepítve. Az alkalmazásfiók nem szerveradminisztrációs fiók.

## Legutóbbi fejlesztés – 01A, 2026-11-17

A JPA/PWA `dae1224` kódjára épített fiókfejlesztés: [815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c](https://github.com/borosdavid/borosCode/commit/815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c). Felhasználónév/jelszó, egyenlő közös alkalmazásjog, BCrypt-hash, session/CSRF, fiókletiltás és jelszócsere utáni online session-visszavonás, újrabelépéskor megőrzött nyitott piszkozat/fotó, fiókváltás ellen védett írás. Első fiókot helyben kell létrehozni; nincs alapjelszó.

Maven verify és 18 Java-teszt sikeres; a PWA fixture 4 csoportja Headless Chromium 131 alatt sikeres. A korábban jelzett Windows/Edge offline hibát ez nem zárja le. A módosított MySQL/Windows harness lefordult, de a valódi MySQL V3-migráció/karbantartás, Windows helper és acceptance, Docker, Tailscale/HTTPS és iPhone még nem volt ellenőrizhető. A natív explicit HTTPS-proxykonfiguráció tesztelve van szimulált kérésekkel; a Docker távoli proxy-topológiája külön szervertelepítési feladat.

Részletes, kódhoz kötött riport és fióklétrehozás: [01A átadási leírás](https://github.com/borosdavid/borosCode/blob/815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c/docs/fiokok-01a.md). A `teszt` ág és a kiadási címkék változatlanok. Következő gyakorlati lépés a valódi izolált adatbázisos/eszközös ellenőrzés, valamint a 01B szerverkapcsolat előkészítése; a 02/03 offline fázisok még nem indultak el.
