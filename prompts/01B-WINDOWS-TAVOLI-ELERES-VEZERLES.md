# 01B – Dedikált Debian-szerver távoli elérése és vezérlése, Windows/iPhone fejlesztéssel

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** D-007 alapján a Debian-laptop dedikált szerver, a fejlesztés Windowsról és iPhone-ról történik. A hardver, a telepítés és a tényleges vezérlési kapcsolat még nem igazolt.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


## Cél

A felhasználó következő feladata: a dedikált Debian-laptopon futó Otthontár szerveroldali alkalmazásának és naplóinak elérése, valamint a Windowsról és iPhone-ról kezdeményezett fejlesztési munka követése; a szerver indítása, leállítása és újraindítása ebből a ChatGPT-beszélgetési környezetből is. A telefonos PWA HTTPS-elérése is erre a működő szerverkapcsolatra épüljön.

Ez előkészített feladat, nem már kiépített hozzáférés. Az első otthoni telepítés és fiók/eszköz-hitelesítés helyi közreműködést igényelhet. Bekapcsolt, elérhető Debian-szerver kell; kikapcsolt gép távoli ébresztése külön feladat. A Windows-fejlesztőgép kikapcsolása ne állítsa le a szervert.

A feladat a [D-007 döntést](../projekt/DONTESI-NAPLO.md) követi: Debian 13/Xfce, Docker Engine/Compose, Tailscale/SSH és privát HTTPS a tervezett alap. Az iPhone-ról kezdeményezett fejlesztés végrehajtási/build helyét külön igazold; ne feltételezz iPhone-on futó helyi buildet. A fájl korábbi Windowsos neve a meglévő hivatkozások miatt megmarad, a célpontot a jelenlegi tartalom határozza meg.

## Kiválasztott tervezési irány és előfeltételek

Kövesd a [konkrét távoli hozzáférési tervet](../projekt/SZERVER-TAVOLI-HOZZAFERES.md). Dávid közvetlen Tailscale/SSH-hozzáférése és a ChatGPT számára külön privát GitHub-műveletsor készülhet; a helyi ügynök csak rögzített műveleteket fogyaszthat, nem Gitből letöltött kódot. A privát repo javasolt neve még nem létrejött szolgáltatás. Az ügynök implementációja a dev ágra kerül majd; a terv nem aktivál vezérlést.

A kezdeti körben status/diagnostics/start/stop/restart készül, csak izolált tesztkörnyezetre. A kódbuild, telepítés és nyers naplóhozzáférés követelménye külön későbbi ütem; ennek lezárásáig a tágabb 01B feladat aktív marad. Általános shell-végrehajtást vagy self-hosted CI-runnert ne telepíts automatikusan a személyes szerverre.

A védett telefonos alkalmazáselérés előtt a [01A fiókfeladat](01A-FELHASZNALOI-FIOKOK.md) készüljön el: minden aktív bejelentkezett felhasználó egyenlő alkalmazásjogot kap, szerveradmin-jogot nem.

## Első lépés: igazolt kapcsolat

- Vizsgáld meg az aktuális dev ágat, az AGENTS.md-t, a közös promptutasításokat, az indító/leállító PowerShell-szkripteket és a docs/pwa-01.md korlátait.
- Mérd fel a Debian-laptop hardverét, jelenlegi rendszerét és a választott összeállítás kompatibilitását. Készíts konkrét helyi telepítési tervet; meglévő adatok törlését vagy lemezformázást külön jóváhagyás nélkül ne végezz.
- A Windows-fejlesztőgépen őrizd meg a helyi profilokat és a még nem pusholt JPA-fejlesztést. A PowerShell-szkriptek a Windows fejlesztési folyamatot szolgálják; a Debian-szerverhez a tényleges Compose/szolgáltatáskezelésre építs.
- Ellenőrizd, az aktuális ChatGPT-környezet milyen tényleges eszközzel tud hitelesített kérést vagy távoli feladatot végrehajtani. A Tailscale a telefon, a Windows és a Debian-szerver között önmagában NEM kapcsolja a ChatGPT futtatókörnyezetét a tailnethez, és nem ad távoli parancsvégrehajtást.
- Először bizonyíts egy minimális, csak olvasó állapotlekérést a valódi Debian-szerverről ebbe a környezetbe. Enélkül ne jelents kész távoli hozzáférést. Ha nincs megfelelő eszköz/csatlakozó, a hiányzó kapcsolatot és a helyi lépést nevezd meg pontosan; ne adj nem működő vagy kitalált URL-t.
- Válassz támogatott, hitelesített vezérlési csatornát. Lehet meglévő megfelelő távoli eszköz vagy helyi, kifelé kapcsolódó végrehajtó. A döntés a tényleges elérhetőségen alapuljon; ne építs automatikusan tetszőleges parancsot futtató publikus HTTP-végpontot.

## Elvárt műveletek

1. Állapot: elérhető-e a gép, fut-e inventory/imaging/adatbázis, mely profilban, milyen verzióval/commitból, mi a health állapot.
2. Indítás, kulturált leállítás és újraindítás a Debianon ellenőrzött Docker Compose/szolgáltatáskezelésre építve. Alapértelmezésként izolált tesztprofilt használj; a személyes példányt soha ne váltsd át csendben. Ismételt indítás ne hozzon létre párhuzamos példányt.
3. Visszakereshető műveletazonosító, időpont, folyamat/szolgáltatás-állapot, kilépési kód és érthető hiba. Legyen időkorlát, kölcsönös kizárás az ütköző műveletekre, és kapcsolatvesztés után lekérdezhető eredmény. A siker ne csupán a parancs elküldését jelentse: health ellenőrzés kell.
4. Korlátozott, releváns alkalmazás/build/tesztnapló olvasása. Ne exportálj titkot, személyes adatot vagy a gépek teljes fájlrendszerét; a nyilvános repóba ilyen napló nem kerülhet.
5. A kijelölt dev/teszt commitból ellenőrzött buildtermék előállítása a Windows fejlesztőgépen vagy igazolt távoli buildkörnyezetben, majd biztonságos telepítése és indítása a Debian-szerveren, a futó verzió pontos jelzésével. A laptop dedikált szerver, ne tedd napi fejlesztői munkakörnyezetté. Ne legyen automatikus destruktív adatbázis-visszaállítás vagy séma-downgrade.
6. Fejlesztési állapotmegosztás: commitok, módosított fájlok, elvégzett és blokkolt tesztek, futó feladatok, utolsó ellenőrzött eredmény. A megosztható, személyes adatot nem tartalmazó összefoglaló kerüljön közös helyre. A másik asztali ChatGPT-beszélgetés belső állapota nem válik automatikusan láthatóvá: az ottani munkát kifejezett riportolással/naplózással kell követhetővé tenni.

## Tartós szerverüzem

- A Docker és az alkalmazás induljon újraindítás után; hálózati táplálás mellett a fedéllecsukás és az üresjárat ne altassa el a szervert.
- A személyes és tesztadatbázis/képtároló legyen elkülönítve; a lemez és a fotók tartós tárolását igazold.
- Legyen a laptopon kívüli adatbázis- és fotómentés, ellenőrzött visszaállítással. A célhely és jogosultságok még eldöntendők; ne válassz automatikusan fizetős tárhelyet.
- A Windows és iPhone fejlesztési munkafolyamat Git-commitokkal és riportokkal kapcsolódjon a szerver telepített verziójához.

## HTTPS és jogosultság

- Különítsd el a PWA felhasználói hozzáférését a szerver adminisztratív vezérlésétől. Az alkalmazás megnyitása ne adjon szerveradminisztrációs jogosultságot.
- A jelenlegi LocalAccessFilter távoli/proxy blokkolását csak megvalósított, ellenőrzött felhasználónév/jelszó + session alapú alkalmazásbelépésre és megbízható proxykezelésre cseréld. Ne kapcsold ki pusztán a link működéséért.
- A Debian oldali végrehajtó csak előre meghatározott műveleteket fogadjon, minimális jogosultsággal, visszavonható hitelesítéssel. Ne legyen tetszőleges shell-parancs, fájlútvonal vagy Git-ref korlátlan átadása. Titkok a helyi biztonságos konfigurációban legyenek, ne repóban, naplóban vagy chatben.
- Ne nyiss nyilvános MySQL/imaging/admin/shell portot. A Debian-szerver privát telefonos eléréséhez Tailscale Serve és megbízható HTTPS használható, Funnel nélkül; az aktuális telepített súgót ellenőrizd. A ChatGPT vezérlési csatornáját ettől külön igazold.
- Önállóan futó, állandó távoli felügyeletet ne ígérj: az interaktív chatből kiadott műveletet, a helyi tartós szolgáltatást és az esetleges későbbi ütemezést egyértelműen különítsd el.

## Elfogadási próba

- Ebből a ChatGPT-környezetből lekérhető a valódi Debian tesztpéldány állapota és egy ártalmatlan, azonosítható tesztnapló.
- Innen indítunk egy leállított tesztpéldányt, megvárjuk a sikeres health ellenőrzést, ellenőrizzük a commitot, majd újraindítjuk és leállítjuk.
- Ismételt indítás, egyidejű művelet, kieső gép, lejárt/hibás hitelesítés és sikertelen build érthető, igazolt eredményt ad. Jogosulatlan vezérlés elutasítva.
- A telefonról megnyitható HTTPS-cím valós alkalmazást szolgál ki megfelelő jogosultsággal; telefonos megnyitás külön ellenőrzendő.
- Az asztali munka végén készített tesztösszefoglaló innen elérhető, de nincs állítás más beszélgetés automatikus teljes láthatóságáról.
- A személyes adatbázis/fotók/profil megmaradnak; a kezdeti próbák izolált tesztadatokon történnek.

## Mentés és átadás

Minden fejlesztést automatikusan commitolj/pusholj a dev ágra az AGENTS.md szerint. A teszt ágra csak valóban kipróbálható verzió kerülhet, pontos Debian-telepítési, Windows/iPhone-csatlakozási, indítási és visszavonási útmutatóval. A végén add meg az ellenőrzött HTTPS-címet, a ténylegesen működő távoli műveleteket, a még helyi beavatkozást igénylő lépéseket és a teszteredményeket.
