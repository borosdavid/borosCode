# Otthontár tesztági átadásjelölt – 01D és előzményei

| Adat | Érték |
|---|---|
| Dátum | 2026-11-27 |
| Tervezett annotált címke | `teszt-2026.11.27.1` |
| Forrás dev kód | [760bd81](https://github.com/borosdavid/borosCode/commit/760bd81b95568867ade5862662910e7f2bd93991) |
| Felhasznált tudástár-verzió | [1ea9107](https://github.com/borosdavid/borosCode-knowledge/commit/1ea910772c29123392ad4e45db107156bdb85a4e) |
| Korábbi címkézett tesztkiadás | Nincs |
| Ellenőrzési környezet | Linux, Java 21, MySQL 8.4.8, Chromium, PowerShell 7 |

## Változáslista

- JPA, fiókok, PWA-alap, a Windows-tesztkörnyezet segédprogramjai és a 01C webes felület.
- A Tárgyak lista- és fotónézetében az egész borítókép középen látszik. A tárgyadatlap mentett és feltöltésre kiválasztott képei ugyanezt az igazítást használják. A mentett kép továbbra is megnyitható eredeti méretben.
- Az iPhone telepítési útmutatója iPhone Safari-lapon jelenik meg. Az Ubuntu mentő- és ellenőrzőszkriptek és a systemd időzítő mintái a 01D implementációjához tartoznak.

## Ellenőrzések

- A teljes Chromium PWA-tesztcsomag sikeres a gyökéren és az `/otthontar` előtaggal. Ellenőrizte a tárgykártyákat, a mentett és kiválasztott álló/fekvő fotók kereteit, a betöltési és munkamenet-hibákat, illetve az offline indítófelületet.
- Az Ubuntu mentési fixture sikeres. A mentés, az ellenőrzőösszeg, a sikertelen mentés, a párhuzamos futás és a hibás archívum ágait ellenőrizte. A Bash-szintaxis és a GitHub Actions workflow lintje sikeres.
- A PowerShell-teszt 77 ellenőrzése sikeres Linuxon. A valódi Windows/Tailscale kapcsolat még eszközös próba.
- A Maven build, 23 Java-egységteszt és 16 MySQL 8.4.8 integrációs teszt sikeres. A becsomagolt alkalmazáson az első fiók létrehozása, belépés, fotófeltöltés, előnézet és újraindítás utáni visszaolvasás is sikeres az `/otthontar` előtaggal.

## Elérés és kézi próbák

Windows natív tesztprofilban, a friss `teszt` ágon futtasd a `scripts/Build.ps1` majd a `scripts/TestServer.ps1 -Action start` parancsot. A web a `http://127.0.0.1:18740/leltar` címen érhető el. Első tesztfiókhoz futtasd a `scripts/Account.ps1 -TestRun -Action create -UserName tesztelo` parancsot. A profil saját MySQL-sémát és képtárolót használ. iPhone-hoz a dokumentált Tailscale HTTPS-címet és a tesztfiókot használd. [Windows és telefonos próba](../windows-tesztszerver-01b.md)

| Próba | Elvárt eredmény |
|---|---|
| Adj egy próbatárgyhoz álló és fekvő képet. Nézd meg a Tárgyak oldal két nézetében, majd a tárgy adatlapon. | A képek középre igazítva, vágás nélkül férnek a keretbe. A mentett képre kattintva megnyílik az eredeti. |
| Nyisd meg iPhone Safari-lapon, majd indítsd a főképernyő ikonról. | A telepítési útmutató a Safari-lapon látható. A főképernyős appban és asztali böngészőben rejtve marad. |
| Ellenőrizd az Ubuntu időzítő első automatikus mentését, majd állíts vissza egy fotót külön tesztmappába. | Az időzített futás új archívumot készít. A visszaállított fotó ellenőrzőösszege megegyezik az eredetivel. [Ubuntu 01D útmutató](../KNOWHOW-ubuntu-napi-mentes.md) |

## Korlátok és visszaállítás

Az éles Ubuntu build frissítése és az iPhone Safari-próba külön lépés. A 01D élő, automatikus mentésének és fotó-visszaállításának bizonyítéka továbbra is szükséges. A `teszt` kódja izolált profilon próbálható. Visszaállításhoz állítsd le ezt a tesztpéldányt, és a korábban rögzített kódból építs újra. A sémamigráció vagy a fotók visszaállításához külön mentés kell.

Visszajelzésnél add meg az ág commitját, az eszközt és a lépést.
