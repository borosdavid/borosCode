# AI-támogatott fejlesztés és otthoni szerverautomatizálás

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** Távlati terv; megvalósíthatósági vizsgálat és részletes döntések szükségesek.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


**Rögzítve:** 2026-11-17  
**Kapcsolódás:** Otthontár Java/Spring Boot backend, iOS PWA és későbbi Android-verzió; otthoni, állandóan működő Linux-szerver

## Cél és felhasználói élmény

Az Otthontár fejlesztése és későbbi üzemeltetése távolról, akár iPhone-ról is kezdeményezhető legyen. A felhasználó természetes nyelven kérhessen funkciófejlesztést, hibakeresést, tesztelést, szerverállapot-ellenőrzést vagy telepítés-előkészítést. Az AI ügynök csak ellenőrizhető, szűk jogosultságú műveleteket végezhessen; az éles környezetet érintő kockázatos változtatások jóváhagyáshoz kötöttek.

Példafeladat: „Vizsgáld meg az Otthontár backend elmúlt 24 órás naplóit, azonosítsd a hibákat, készíts javítást külön ágon, futtasd a teszteket, de az éles szervert jóváhagyás nélkül ne módosítsd.”

**Ez a dokumentum elképzelés és követelménygyűjtemény, nem végrehajtandó Codex-prompt, és nem állítja, hogy a leírt automatizmusok már működnek.**

## Tervezett architektúra

1. **Távoli kezdeményezés:** ChatGPT/Codex támogatott felületen (akár mobilról); a tetszőleges saját SSH-munkamenet közvetlen ChatGPT-vezérléséhez külön integráció és jogosultságkezelés szükséges.
2. **Forráskód és feladatkezelés:** GitHub-repository, issue-k, branch-ek, pull requestek; Codex CLI vagy támogatott Codex-munkakörnyezet kódmódosításhoz, parancsfuttatáshoz és teszteléshez. A GitHub-kapcsolat olvasási képességei önmagukban nem jelentenek szerverhozzáférést vagy kódfuttatási jogosultságot.
3. **Automatizált ellenőrzés:** GitHub Actions CI-folyamat a Java buildhez, unit/integrációs tesztekhez, Docker image építéséhez és biztonsági ellenőrzéshez; igény esetén Codex Action körülhatárolt feladatokra.
4. **Telepítési csatorna:** sikeres ellenőrzések után jóváhagyással, elkülönített technikai jogosultsággal telepíthető verzió. Az otthoni szerverre történő elérés és a titkok kezelése külön megtervezendő; ne nyissunk nyilvános Docker- vagy SSH-adminisztrációs végpontot.
5. **Otthoni Linux-szerver:** a meglévő, erre dedikált laptopon Debian 13 (esetleg könnyű Xfce felület), Docker Engine + Compose, Java Spring Boot, külön képfeldolgozó szolgáltatás, MySQL, tartós adat- és képtárolás.
6. **Biztonságos adminisztráció:** Tailscale és SSH; szükség esetén saját, korlátozott MCP-eszköz vagy parancsburkoló az állapotlekérdezéshez és előre engedélyezett karbantartási műveletekhez.
7. **Megfigyelés és helyreállítás:** naplózás, Uptime Kuma-szerű elérhetőségfigyelés, Restic-szerű titkosított, külső helyre mentés és rendszeresen próbált visszaállítás; később Ansible az ismételhető konfigurációkezeléshez.

A Linux/ChatGPT asztali alkalmazás, a Codex CLI, a laptop hardverkövetelményei, az MCP- és mobilintegráció aktuális kompatibilitása **telepítés előtt ellenőrizendő**; ezek nem feltételezett kész adottságok.

## Automatizálási lehetőségek

| Művelet | Javasolt megoldás | Ellenőrzés |
| --- | --- | --- |
| Spring Boot funkció fejlesztése | Codex + GitHub branch/PR | Kódreview, tesztek |
| Unit- és integrációs tesztek | GitHub Actions | Kötelező sikeres CI |
| Docker build és kiadás-előkészítés | GitHub Actions + Compose | Verziózott image, telepítési jóváhagyás |
| Backend- és konténernaplók elemzése | Korlátozott SSH/MCP + Codex | Naplók maszkolása, csak olvasás alapértelmezésben |
| Adatbázis- és képfájlmentés | Ütemezett mentés + külső tárhely | Mentés- és visszaállítási próba |
| Szerver- és szolgáltatásfigyelés | Monitoring és értesítések | Riasztási küszöbök, hamis riasztások |
| Linux konfiguráció / frissítések | Ansible + felügyelt műveletek | Előzetes terv, jóváhagyás, rollback |
| Hibajavítás előkészítése | Codex külön branch-en | PR és jóváhagyás; nincs önkényes éles módosítás |

## Biztonsági és működési alapelvek

- Az AI-nak **ne legyen általános root-, korlátlan Docker-socket- vagy éles adatbázis-adminisztrátori hozzáférése**; használjon elkülönített, minimális jogosultságú fiókokat.
- Diagnosztika, naplóolvasás és tesztelés lehet automatizált; éles telepítéshez, destruktív művelethez, adatbázis-migrációhoz és kockázatos rendszerfrissítéshez külön jóváhagyás szükséges.
- GitHub Actions titkok, SSH-kulcsok, API-tokenek és személyes adatok ne kerüljenek a repositoryba, a promptokba vagy a naplókba. A GitHub és a szerver hitelesítése külön kezelendő.
- Az alkalmazás publikus elérése HTTPS-en, megfelelő hitelesítéssel és hozzáférésvédelemmel történjen; az adminisztráció privát csatornán.
- A meglévő adatbázis, képek és Docker-kötetek maradjanak meg. Verziózott, tesztelt mentés és visszaállítás legyen meg **mielőtt** automatikus telepítést engedélyezünk.
- Az automatikus indulás, leállás utáni helyreállás, naplórotáció, szabad tárhely és hálózati kiesés kezelése is része az üzemeltetésnek.
- A projekt jelenlegi Git-munkarendje érvényes: dokumentáció/fejlesztés a `dev` ágra; `teszt` csak ténylegesen felhasználói tesztelésre kész állapotban; `main` nem módosul automatikusan.
- A GitHub Actions felhőben is futtathat teszteket, de az otthoni szervernek az éles műveletekhez elérhetőnek kell lennie. A közvetlen hozzáférés helyett biztonságos, kifelé kezdeményezett telepítési kapcsolat is vizsgálható.

## Javasolt bevezetési fázisok

1. **Alapok:** a régi laptop hardverének ellenőrzése; Debian, Docker Compose, SSH/Tailscale, HTTPS, tartós adattárolás, automatikus indulás, külső mentés és sikeres visszaállítási próba.
2. **Fejlesztői automatizálás:** Codex CLI, GitHub-repository és CI-tesztek; dokumentált build- és ellenőrzési folyamat.
3. **Biztonságos kiadás:** verziózott artefaktum, telepítési jóváhagyás, visszagörgetési terv, állapotellenőrzések.
4. **Távoli AI-üzemeltetés:** szűk körű, elsősorban csak olvasási MCP/SSH-eszközök; napló- és monitoring-alapú hibadiagnózis, mobilról kezdeményezhető kérések.
5. **Haladó automatizálás:** jóváhagyott javítási PR-ek, ütemezett felügyeleti összefoglalók és korlátozott önhelyreállító műveletek, csak megfelelő auditálás mellett.

## Nyitott döntések

- A dedikált laptop processzora, memóriája, háttértára és energiafogyasztása alkalmas-e a teljes Spring Boot + képfeldolgozó + MySQL stackhez?
- Kell-e grafikus Linux-felület, vagy a Codex CLI és SSH önmagában elegendő?
- Mely támogatott ChatGPT/Codex mobilos és távoli munkamenet-kapcsolat használható ténylegesen a kiválasztott fiókkal?
- GitHub Actions runner, saját runner vagy szerveroldali, kifelé kezdeményezett telepítési ügynök legyen?
- Pontosan milyen parancsokat engedjen az MCP/SSH köztes réteg, milyen auditnaplóval?
- Hol legyen a külső mentési cél, mennyi legyen a megőrzési idő és milyen gyakran legyen visszaállítási gyakorlat?
- Milyen tesztkapuk és visszagörgetési feltételek után engedhető éles telepítés?

## A későbbi megvalósítás elfogadási feltételei

- Telefonról indított kérésből létrejöhet külön branch/PR, és a CI bizonyíthatóan lefut.
- A szerver állapota és a naplók távolról, korlátozott jogosultsággal lekérhetők, titkok kiszivárgása nélkül.
- Az éles telepítés jóváhagyást és sikeres ellenőrzést igényel; hibánál dokumentáltan visszaállítható.
- A képek és az adatbázis külső mentésből próbakörnyezetben helyreállíthatók.
- Megszakadt internet, áramkimaradás vagy hibás konténer után a rendszer a meghatározott módon áll helyre.

**Nem jelenleg végrehajtandó fejlesztési vagy telepítési megbízás.** A részfeladatokhoz később önálló, ellenőrizhető Codex-promptokat készítünk.
