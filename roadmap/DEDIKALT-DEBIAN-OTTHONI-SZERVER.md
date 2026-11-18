# Dedikált Debian otthoni szerver – kialakítás és üzemeltetés

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** A régi laptop pontos hardveradatai és a hálózati környezet még nem ismertek; a technológiai választások hardver- és támogatási ellenőrzést igényelnek. Az átmeneti Windows-tesztszerver külön, aktív 01B feladat.  
> **Megvalósítás:** Nincs igazolt szervertelepítés vagy megvalósítási commit.  
> **Ellenőrzés:** A követelmények és a megvalósítási terv dokumentálva; valós gépes telepítés és visszaállítási teszt még nem történt.  
> **Felhasználói teszt:** Nincs átadva.  
> **Frissítve:** 2026-11-18. [Státuszszabályok](../projekt/STATUSZOK.md).

## Cél és hatókör

Egy régi, kizárólag Linuxszal használható laptopból állandóan üzemelő, biztonságos, távolról elérhető Otthontár-szerver kialakítása. Hosszabb távon további alkalmazások futtatására is legyen alkalmas. A cél: megbízható működés, tartós adatok, HTTPS, jogosultságvédelem, automatikus indulás, külső titkosított mentés és bizonyított visszaállíthatóság. A telepítési és karbantartási munkák helyi Codex-munkamenetből támogatottak legyenek.

**Nem végrehajtási megbízás.** A konkrét telepítés külön, ellenőrizhető Codex-feladatokkal indul a hardverfelmérés és a döntések után.

## Tervezett rétegek

- **Alaprendszer:** Debian 13, opcionális könnyű Xfce asztali felület; a grafikus környezet csak akkor szükséges, ha helyi grafikus ChatGPT/böngészős kezelés valóban hasznos és a hardver elbírja. Headless telepítés is vizsgálandó.
- **Konténerplatform:** Docker Engine + Compose, megfontolt erőforráskorlátok, állapotellenőrzések, újraindítási szabályok és naplórotáció.
- **Alkalmazások:** Java Spring Boot backend, külön képfeldolgozó szolgáltatás, MySQL; a kép- és adatbázistárolás tartós köteteken vagy megfelelő hostkönyvtárakban.
- **Publikus szolgáltatás:** HTTPS reverse proxy (javaslat: Caddy), alkalmazásszintű hitelesítés és jogosultság; Cloudflare Tunnel megvizsgálandó portnyitás nélküli publikáláshoz, különösen CGNAT esetén. Domain, szolgáltatási feltételek, függőségek és alternatívák tisztázandók.
- **Privát adminisztráció:** Tailscale + SSH, kulcsalapú hitelesítés, minimális jogosultságok, közvetlen publikus SSH-, MySQL-, Docker- és adminportok nélkül.
- **Fejlesztői környezet:** Git, GitHub, hivatalos Codex CLI; a hivatalos Linuxos ChatGPT alkalmazás tényleges elérhetőségét, terjesztési csatornáját és hardver/OS-támogatását telepítéskor ellenőrizni kell. A grafikus alkalmazás hiánya ne blokkolja a CLI-t.
- **Mentés és felügyelet:** Restic vagy hasonló titkosított, verziózott külső mentés; rendszeres próbavisszaállítás; systemd, naplózás és későbbi monitoring/riasztás.

## Elérési és biztonsági modell

1. A webes és mobilos Otthontár-kliens HTTPS-en az alkalmazást éri el, **nem** a szerver adminisztrációs felületét. A hálózati védelem nem helyettesíti az alkalmazás beléptetését.
2. A szerver SSH-, Compose-, adatbázis- és naplókezelése csak privát, hitelesített csatornán lehetséges. A Tailscale önmagában nem ad ChatGPT-nek szerverhozzáférést.
3. Codex CLI helyben szerkeszthet konfigurációt, készíthet telepítési tervet, futtathat ellenőrzéseket és az engedélyezett műveleteket. Ne kapjon alapból korlátlan root- vagy Docker-socket-hozzáférést.
4. A későbbi **telefonról indított ChatGPT-adminisztrációhoz** külön működő, hitelesített integráció szükséges (például szűk jogú MCP/ügynök/API). Kezdetben csak status, health, szűrt naplók; start/stop/restart kizárólag előre definiált parancsokkal, auditnaplóval, explicit engedélyekkel. A destruktív műveletek, adatbázis-migrációk, frissítések és éles telepítések jóváhagyáskötelesek.
5. Titkok, tokenek és SSH-kulcsok nem kerülhetnek a Git-repóba, naplókba vagy promptokba. Személyes képek és adatok megőrzése elsőbbséget élvez.

## Adatbiztonság és helyreállíthatóság

- Különítsük el a konténer életciklusát az adatbázis és képek tartós tárolásától.
- Napi konzisztens MySQL-mentés; képek és a visszaállításhoz szükséges konfigurációk összehangolt mentése.
- Titkosított mentés **másik fizikai eszközre vagy külső felhőtárhelyre**, megőrzési szabályokkal és a mentési titkok külön biztonsági kezelésével.
- Dokumentált próbavisszaállítás elkülönített környezetben; hibás/frissített verzióra rollback-terv.
- Vizsgáljuk az áramszünetet, hálózati megszakadást, lemezbetelést, hardverhibát és megsérült adatbázist; egyetlen sikeres mentés önmagában nem elegendő.

## Megvalósítási fázisok és ellenőrzési kapuk

1. **Hardverfelmérés:** pontos laptopmodell, CPU/architektúra, RAM, háttértár típusa/állapota, hálózat, BIOS/UEFI, akkumulátor, hűtés, fogyasztás. Hasznos Linux-parancsok: `lscpu`, `free -h`, `lsblk -o NAME,SIZE,TYPE,MODEL`, `df -h`, `upower -e`. A 64 bites CPU, SSD és 8 GB RAM tájékozódási célérték, nem igazolt minimum.
2. **Debian telepítés és alapvédelem:** Debian 13, szükség esetén Xfce; külön normál felhasználó, frissítések, SSH-kulcsok/tűzfal; a fedél lecsukásakor alvás letiltása, áramkimaradás utáni indulás lehetőségének vizsgálata.
3. **Codex és fejlesztői eszközök:** hivatalos telepítési út és kompatibilitás ellenőrzése; Git/GitHub hitelesítés, CLI működési próba, korlátozott jogosultságok.
4. **Docker-alapok:** Compose telepítése, Spring Boot + imaging + MySQL stack, tartós kötetek, egészségellenőrzés, automatikus újraindítás; adatvesztést kizáró telepítési teszt.
5. **Elérés és TLS:** Tailscale/SSH privát adminisztráció; Caddy és a kiválasztott publikus hozzáférési megoldás, valós külső HTTPS és alkalmazásbelépési teszt.
6. **Üzemeltetés és mentés:** naplók/monitoring, tárhelyfigyelés, külső mentés; sikeres visszaállítási próba és áram-/hálózati kiesés utáni helyreállás ellenőrzése.
7. **Verziózott telepítés:** GitHub `dev` fejlesztések, ellenőrzött build/CI, jóváhagyott deploy és visszagörgetés; csak felhasználói tesztre kész kód kerül a `teszt` ágra. A `roadmap/` és `prompts/` hiteles dokumentációja a `main` ágon marad.
8. **Mobilos ChatGPT-vezérlés:** külön hitelesített kapcsolat; státusz/napló, majd ellenőrzött start/stop/restart és telepítési műveletek, élő végponttól kapott visszaigazolással.

## Kapcsolódó anyagok és sorrend

- [AI-támogatott fejlesztés és szerverautomatizálás](AI-FEJLESZTES-ES-SZERVERAUTOMATIZALAS.md) – a későbbi mobilos és AI-munkafolyamatok tágabb koncepciója.
- [01B – átmeneti Windows-tesztszerver](../prompts/01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md) – jelenleg külön folyamat; a PowerShell-megvalósítás **nem** másolható változtatás nélkül Linuxra.
- [Projektállapot](../projekt/ALLAPOT.md) és [döntési napló](../projekt/DONTESI-NAPLO.md) – D-011 szerint a Windows-tesztszerver az átmeneti megoldás, a dedikált Debian laptop későbbi lépés.

## Nyitott döntések / elfogadási feltételek

**Nyitott:** hardver megfelelősége; Xfce szükségessége; publikus domain/TLS/tunnel stratégia; külső mentési cél és retenció; CI/deploy mechanizmus; ChatGPT-Linux és Codex valóban támogatott telepítési útja; a mobilos ügynök pontos hitelesítése és jogosultságai.

**A teljes terv akkor tekinthető késznek, ha** a backend, imaging és MySQL tartósan üzemel; HTTPS és hitelesítés működik távolról; adminisztráció privát; az automatikus indulás és kiesés utáni helyreállás próbált; a külső mentésből az adatbázis és képek visszaállíthatók; a telepítés jóváhagyott és visszagörgethető; a mobilos vezérlés valódi, auditált és korlátozott jogosultságú kapcsolaton bizonyított.

A következő tényleges lépés a laptop hardveradatainak megadása és az első telepítési feladat kidolgozása.
