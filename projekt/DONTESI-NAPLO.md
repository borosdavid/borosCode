# Döntési napló

Hiteles forrás: a `main` ág. A napló az elfogadott irányokat és a még eldöntendő javaslatokat külön kezeli. A döntés elfogadása nem bizonyítja a megvalósítás elkészültét; a készültséget a [projektállapot](ALLAPOT.md) és a feladatlapok jelzik.

## Bejegyzések

| Azonosító | Döntés | Döntési állapot | Dátum |
|---|---|---|---|
| D-001 | dev fejlesztési ág és külön tesztátadás | Elfogadott | 2026-11-17 |
| D-002 | main a promptok és roadmapek hiteles forrása | Elfogadott; korábbi szinkronszabály felváltva | 2026-11-17 |
| D-003 | Első iPhone-megoldásként PWA | Elfogadott fejlesztési irány | Rögzítve: 2026-11-17 |
| D-004 | Közös csempés alkalmazásgyűjtő, önálló modulokkal | Elfogadott távlati termékirány | 2026-11-17 |
| D-005 | Státuszok, projektállapot és címkézett tesztátadás | Elfogadott munkarend | 2026-11-17 |
| D-006 | Állandó szerver végleges gépe és hozzáférése | Részben feloldva D-007-tel; telepítési és üzemeltetési részletek nyitottak | 2026-11-17 |
| D-007 | Dedikált Debian-szerver, fejlesztés Windowsról és iPhone-ról | Elfogadott irány; telepítés még nem igazolt | 2026-11-17 |

## D-001 – Fejlesztési és tesztágak

**Döntés:** az alkalmazásfejlesztés automatikus commit/push célja a `dev`. A `teszt` ágra csak a felhasználónak ténylegesen átadható, ellenőrzött feladatkör kerül. A `main` alkalmazáskódja külön kiadáskor frissül.

**Indok:** a folyamatban lévő munkát meg kell őrizni, közben egyértelmű legyen, melyik változat próbálható ki.

**Megfontolt alternatíva:** minden fejlesztési commit automatikus átemelése a tesztágra.

**Következmény:** a tesztág létezése nem bizonyít tesztkiadást; hozzáférési előfeltétel, teszteredmény és átadási leírás is kell.

**Forrás:** a felhasználó 2026-11-17-i ágmunkarendre vonatkozó kérése; [ágkezelési munkarend](../docs/branch-workflow.md).

## D-002 – Tervezési dokumentáció a main ágon

**Döntés:** a promptok, roadmapek, archívumaik és a központi projektirányítási oldalak szerkesztési/hiteles forrása a `main`. A `dev` és `teszt` példányai maradhatnak, de nincs kötelező szinkron. A műszaki és tesztdokumentáció a kódot követi. A közös munkarendi szabályok minden használt ágon aktuálisak.

**Indok:** egyetlen aktuális tervlista; kevesebb párhuzamos szerkesztés és fölösleges dokumentációmásolás.

**Korábbi döntés:** a promptok és roadmapek minden módosításának szinkronizálása mindhárom ágra. Ez a szabály **felváltva**, nem alkalmazandó.

**Következmény:** fejlesztéskor a `main` adott commitjából olvassuk a feladatot; az aktuális terv készültsége eltérhet a main kódjától. Ágakat emiatt nem kell teljes egészében összefésülni.

**Bizonyíték:** a felhasználó kifejezett jóváhagyása; [átállító commit](https://github.com/borosdavid/borosCode/commit/040dad1f0f1f24ab33d824257e8b3e9a03a3f8b7).

## D-003 – iPhone PWA

**Döntés:** az első iPhone-os megoldás a meglévő webes rendszerre épülő, főképernyőre telepíthető PWA, tervezett offline leltárral és későbbi szinkronnal.

**Indok:** a felhasználónak nincs Macje; a cél saját iPhone-os használat App Store-kiadás nélkül, a meglévő backend újrahasznosításával.

**Alternatíva:** natív iOS-alkalmazás. A PWA választása nem zár ki későbbi natív alkalmazást; új igénynél külön mérlegelendő.

**Következmény:** a biztonságos HTTPS-elérés, a Safari/eszközös ellenőrzés és a böngészőtárhelyből eredő adatmegőrzési korlátok külön feladatok. A teljes offline működés nem azonos a jelenlegi PWA-alappal.

**Forrás:** [közös PWA-szabályok](../prompts/00-KOZOS-UTASITASOK.md), [01-es feladat](../prompts/01-PWA-HTTPS-MOBIL-ALAP.md). A korábbi irány pontos elfogadási napját nem állapítottuk meg; a fenti dátum a naplóba rögzítés napja.

## D-004 – Alkalmazásgyűjtő

**Döntés:** távlati cél egy közös webes/iOS/Android kezdőképernyő, csempékről indítható Otthontár-, naptár-, sport/táplálkozás- és játékmodulokkal. Az Otthontár önálló modul marad.

**Indok:** a felhasználó több különálló funkciót szeretne ugyanazon megoldásban összefogni.

**Alternatíva:** a későbbi funkciók közvetlen beépítése a leltár saját felületébe és adatmodelljébe.

**Következmény:** külön funkcionális felelősségek; ez nem automatikus döntés mikroszolgáltatásokról, frontend-keretrendszerről vagy natív appokról. A Snake helyi játékmenete és közös szerveres ranglistája egyelőre specifikálandó terv.

**Forrás:** a felhasználó 2026-11-17-i bővítési kérése; [alkalmazásgyűjtő](../roadmap/ALKALMAZASGYUJTO-KEZDOKEPERNYO.md), [Snake](../roadmap/SNAKE.md).

## D-005 – Követhető állapot és tesztátadás

**Döntés:** egységes életciklus, bizonyítékokkal karbantartott projektállapot, változáslistával kísért és annotált Git-taggel rögzített alkalmazás-tesztátadás, valamint döntési napló.

**Indok:** a több beszélgetésben és gépen folyó munka folytatható legyen, a felhasználói visszajelzés pontos verzióhoz kapcsolódjon.

**Alternatíva:** kizárólag beszélgetési előzmények és mozgó branchnevek használata.

**Következmény:** a fejlesztő az érdemi munkához kapcsolódóan aktualizálja a központi oldalakat. Ez nem folyamatos háttérfigyelés. Kiadási címkét csak valóban átadható alkalmazásverzió kap; a jelenlegi dokumentációs változás nem új PWA-kiadás.

**Forrás:** a felhasználó 2026-11-17-i „ezeket ebben a sorrendben csináld is meg” utasítása; [státuszok](STATUSZOK.md), [projektállapot](ALLAPOT.md), [tesztverziók](TESZTVERZIOK.md).

## D-006 – Állandó szerver és távoli kezelés

**Frissítés, 2026-11-17:** az alábbi eredeti kérdésfelvetés gépszerepekre és alapösszeállításra vonatkozó részét D-007 feloldotta. Az eredeti állapotot előzményként megőrizzük; a hardver, jogosultságok, mentési cél és a tényleges vezérlési csatorna részletei még nyitottak.

**Igazolt cél:** folyamatos szerverháttér, védett internetes hozzáférés, tartós adat- és képtárolás, automatikus indulás és külső, visszaállítható mentés. A felhasználó meglévő Linux-laptop használatát vetette fel.

**Még javaslat:** Debian/Xfce, Docker Compose, Tailscale/SSH és privát HTTPS. A konkrét hardverrel való kompatibilitás, a telepített szoftverek és a ChatGPT-ből ténylegesen használható hitelesített vezérlési csatorna nem igazoltak. Ez a napló nem ellenőrzi az ajánlott termékek aktuális támogatását.

**Nyitott kérdések:** a laptop hardvere; a Windows-fejlesztőgép és Linux-üzemi gép pontos szerepe; a beállítás első helyi lépései; jogosultságok, mentési célhely és elérési mód.

**Megfontolt alternatívák:** bérelt VPS, Raspberry Pi, mini PC és meglévő laptop. Végleges hardver- vagy szolgáltatásvásárlási döntést ez a dokumentációs munka nem hoz.

**Következmény:** a [01B Windows-feladat](../prompts/01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md) átírása vagy végrehajtása előtt a gépszerepeket tisztázni kell. Az azonos fiók és a Tailscale önmagában nem biztosít ebből a beszélgetésből géphozzáférést.

**Forrás:** a szerveres beszélgetésben felvetett célok és alternatívák; [automatizálási távlati terv](../roadmap/AI-FEJLESZTES-ES-SZERVERAUTOMATIZALAS.md). Nem kész telepítés.

## D-007 – Dedikált Debian-szerver és távoli fejlesztés

**Döntési állapot:** Elfogadott irány, 2026-11-17. A D-006 gépszerepekre és javasolt alapösszeállításra vonatkozó nyitott részét feloldja; az üzemeltetési részleteket nem zárja le.

**Döntés:** a meglévő laptop kizárólag szerverfeladatot kap. Tervezett alapja Debian 13/Xfce, Docker Engine és Compose, Tailscale/SSH és privát HTTPS. Az alkalmazásszolgáltatások, a MySQL és a tartós képtárolás ezen a gépen futnak majd, automatikus indulással és külső, visszaállítható mentéssel. Az Xfce a szükség szerinti helyi beállítást segítheti; nem teszi a szervert napi fejlesztőgéppé.

| Eszköz | Szerep |
|---|---|
| Debian-laptop | Dedikált, folyamatosan működő szerver; alkalmazás és adattárolás |
| Windows-gép | Fejlesztés, helyi build és tesztek; a meglévő helyi munka megőrzése |
| iPhone | Fejlesztések kezdeményezése, követése és ellenőrzése ChatGPT/Codex felületen, valamint PWA-próbák; a kódvégrehajtás és build helyét külön kell igazolni |

**Indok:** az alkalmazás elérhetősége ne függjön a Windows-fejlesztőgép bekapcsolásától. A felhasználó Windowsról és iPhone-ról szeretné folytatni a fejlesztést.

**Alternatíva:** a Windows-gép használata folyamatos szerverként, illetve a szerver és napi fejlesztés ugyanazon laptopra helyezése. Ezek helyett a dedikált Debian-szerep lett kiválasztva.

**Következmény:** a 01B feladat szerveroldali célpontja Debian; a Windows-oldali fejlesztési állapot követése megmarad, de a PowerShell-indítók nem Linuxos szerverindítók. A szerverre lehetőleg ellenőrzött buildtermék kerül; a végleges build/telepítési folyamat még kialakítandó. A PWA használata és az adminisztratív vezérlés külön jogosultságú kapcsolat legyen.

**Nyitva marad:** hardver és kompatibilitás, első helyi telepítési lépések, felhasználók/jogosultságok, külső mentés célhelye, és az ebből a ChatGPT-környezetből ténylegesen működő hitelesített vezérlési csatorna. Az iPhone-os fejlesztési igény nem jelent automatikus helyi buildkörnyezetet az iPhone-on.

**Bizonyíték:** a felhasználó 2026-11-17-i pontosítása: „a javasolt összeállítás mentén tervezek menni, a Debianes laptop csak szervernek lesz használva, a fejlesztés windowsról és iphone-ról menne”.

**Megvalósítás:** ez elfogadott terv, nem igazolt telepítés vagy már létrejött távoli hozzáférés. A szoftvertámogatást és kompatibilitást a telepítés előtt ténylegesen ellenőrizni kell.

## Új döntés vagy módosítás

Új bejegyzéshez használd a [döntési sablont](sablonok/DONTES.md). Legyen benne azonosító, dátum, állapot, kérdés, választás, indok, alternatívák, következmények és a felhasználói döntés vagy bizonyíték hivatkozása.

Elfogadott döntést ne írj át csendben: új bejegyzésben hivatkozd és váltsd fel a régit, a régiben jelöld a felváltást. A javaslatból csak tényleges felhasználói döntés vagy már megadott végrehajtási felhatalmazás alapján legyen Elfogadott. Nyitott, a végrehajtást befolyásoló kérdésnél kérdezz; a jóváhagyott rutinmunkát ne állítsd meg újabb megerősítéssel.

Az itt használt döntési állapotok (Javasolt, Elfogadott, Felváltva, Elvetett, Nyitott) különböznek a funkciók megvalósítási státuszától.
