# Projektállapot – Otthontár

**Frissítve:** 2026-11-17. Ez a `main` ágon karbantartott állapotfelvétel, nem élő szervermonitor és nem a `main` kódjának funkciólistája.

## Most hol tartunk?

- A helyi PWA-alap a `dev` ágon van; a telefonról megnyitható, védett HTTPS-elérés még nincs igazolva.
- A teljes offline leltár, a tartós offline szerkesztés és a kétirányú szinkron még további fejlesztés.
- A távoli `dev` ágon a JPA-átállás nem látható; az esetleges Windows-oldali helyi eredményt innen nem tudjuk ellenőrizni.
- A `teszt` ág jelenlegi alkalmazáskódja induló alapállapot, nem átadott PWA-tesztkiadás.
- A mostani státuszrendezés dokumentációs változás; nem indítottunk alkalmazást, nem módosítottunk személyes adatot, nem futtattunk új alkalmazásteszteket.

## Következő teendő és akadályok

1. A korábban kijelölt 01B feladat előkészítése: a tényleges fejlesztői/szerverkörnyezet azonosítása és hitelesített kapcsolat kialakítása.
2. A Windows-fejlesztőgépen esetleg meglévő JPA-munka biztonságos ellenőrzése, mentése és összefésülése. Az offline fázisok előtt ezt rendezni kell.
3. Az 01-es fázis hozzáférésvédelmének és telefonos HTTPS-elérésének befejezése, majd tesztkiadás.
4. Ezután a teljes offline olvasás (02), offline írás/szinkron (03), végső iPhone-próbák (04).

**Nyitott egyeztetés a szerveres szálhoz:** a 01B jelenleg Windows-gépet ír le; közben felmerült egy külön Linux-laptop mint állandó szerver. A laptop típusa/RAM-ja, a végleges gépszerepek és a ténylegesen használható távoli kapcsolat nem igazolt. A szerveres tervezés külön beszélgetésben folytatható, de a megállapodás és bizonyíték a repóba is kerüljön. Tailscale vagy azonos ChatGPT-fiók önmagában nem ad ebből a beszélgetésből géphozzáférést.

## Végrehajtási feladatok

| Feladat | Státusz | Igazolt eredmény / következő feltétel |
|---|---|---|
| [00A – JPA](../prompts/00A-HIBERNATE-JPA-ATALLAS.md) | Tervezett | Távoli megvalósítás nem igazolt; a helyi Windows-munka ellenőrzendő |
| [01 – PWA és HTTPS](../prompts/01-PWA-HTTPS-MOBIL-ALAP.md) | Fejlesztés alatt | Helyi PWA kész; hozzáférés és valódi iPhone-átadás hiányzik |
| [01B – távoli vezérlés](../prompts/01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md) | Tervezett | Kapcsolat és végleges gépszerepek még nem igazoltak |
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

- [PWA-riport](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/docs/pwa-01.md): korábbi, 2026-11-17-i 10 sikeres Java-teszt és 3 sikeres böngészős tesztcsoport. Ebben a dokumentációs feladatban ezeket nem futtattuk újra.
- [Ledger](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/inventory/src/main/java/hu/boros/inventory/Ledger.java) és [inventory/pom.xml](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/inventory/pom.xml): JdbcTemplate és JDBC starter; ez alapján a távoli JPA-átállás nem igazolt.
- A PWA-riport szerint a Tailscale/proxyforgalom jelenleg 403-at kap; a jelenlegi hozzáférési szűrő nem kész távoli hitelesítés.
- A Windows teljes acceptance, a Docker/MySQL-integráció, a valódi iPhone-próba és a működő szerverkapcsolat ebben a feladatban nem volt ellenőrizhető.

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

Döntések és nyitott javaslatok: [döntési napló](DONTESI-NAPLO.md). A D-006 szerveres kérdés nyitott; a jelenlegi dokumentációs munkát nem akadályozza, a szerver telepítése előtt tisztázandó.
