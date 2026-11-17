# 01B – Windows-szerver távoli elérése és vezérlése ChatGPT-ből

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** A távoli kapcsolat nincs igazolva. A Windows-fejlesztőgép és a tervezett Linux-üzemi szerver szerepét a szerveres szálban tisztázni kell; a meglévő feladatot nem tekintjük automatikusan Linuxra átírtnak.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


## Cél

A felhasználó következő feladata: a Windows-gépen futó Otthontár szerveroldali alkalmazásának, naplóinak és az ott végzett fejlesztési munka állapotának elérése; a szerver indítása, leállítása és újraindítása ebből a ChatGPT-beszélgetési környezetből is. A telefonos PWA HTTPS-elérése is erre a működő szerverkapcsolatra épüljön.

Ez előkészített feladat, nem már kiépített hozzáférés. Az első otthoni telepítés és fiók/eszköz-hitelesítés helyi közreműködést igényelhet. Bekapcsolt, elérhető Windows-gép kell; kikapcsolt gép távoli ébresztése külön feladat.

## Első lépés: igazolt kapcsolat

- Vizsgáld meg az aktuális dev ágat, az AGENTS.md-t, a közös promptutasításokat, az indító/leállító PowerShell-szkripteket és a docs/pwa-01.md korlátait.
- Mérd fel a Windows-gépen a natív/Docker futtatási módot, a személyes és izolált tesztprofilokat, a folyamatokat, portokat, naplókat és a még nem pusholt JPA-fejlesztést. Módosítás előtt őrizd meg a helyi munkát.
- Ellenőrizd, az aktuális ChatGPT-környezet milyen tényleges eszközzel tud hitelesített kérést vagy távoli feladatot végrehajtani. A Tailscale a telefon és a Windows között önmagában NEM kapcsolja a ChatGPT futtatókörnyezetét a tailnethez, és nem ad távoli parancsvégrehajtást.
- Először bizonyíts egy minimális, csak olvasó állapotlekérést a valódi Windows-gépről ebbe a környezetbe. Enélkül ne jelents kész távoli hozzáférést. Ha nincs megfelelő eszköz/csatlakozó, a hiányzó kapcsolatot és a helyi lépést nevezd meg pontosan; ne adj nem működő vagy kitalált URL-t.
- Válassz támogatott, hitelesített vezérlési csatornát. Lehet meglévő megfelelő távoli eszköz vagy helyi, kifelé kapcsolódó végrehajtó. A döntés a tényleges elérhetőségen alapuljon; ne építs automatikusan tetszőleges parancsot futtató publikus HTTP-végpontot.

## Elvárt műveletek

1. Állapot: elérhető-e a gép, fut-e inventory/imaging/adatbázis, mely profilban, milyen verzióval/commitból, mi a health állapot.
2. Indítás, kulturált leállítás és újraindítás a meglévő szkriptekre építve. Alapértelmezésként izolált tesztprofilt használj; a személyes példányt soha ne váltsd át csendben. Ismételt indítás ne hozzon létre párhuzamos példányt.
3. Visszakereshető műveletazonosító, időpont, folyamat/szolgáltatás-állapot, kilépési kód és érthető hiba. Legyen időkorlát, kölcsönös kizárás az ütköző műveletekre, és kapcsolatvesztés után lekérdezhető eredmény. A siker ne csupán a parancs elküldését jelentse: health ellenőrzés kell.
4. Korlátozott, releváns alkalmazás/build/tesztnapló olvasása. Ne exportálj titkot, személyes adatot vagy a teljes Windows-fájlrendszert; a nyilvános repóba ilyen napló nem kerülhet.
5. A felhasználó által kijelölt dev/teszt commit biztonságos buildelése és indítása, a futó verzió pontos jelzésével. Ne legyen automatikus destruktív adatbázis-visszaállítás vagy séma-downgrade.
6. Fejlesztési állapotmegosztás: commitok, módosított fájlok, elvégzett és blokkolt tesztek, futó feladatok, utolsó ellenőrzött eredmény. A megosztható, személyes adatot nem tartalmazó összefoglaló kerüljön közös helyre. A másik asztali ChatGPT-beszélgetés belső állapota nem válik automatikusan láthatóvá: az ottani munkát kifejezett riportolással/naplózással kell követhetővé tenni.

## HTTPS és jogosultság

- Különítsd el a PWA felhasználói hozzáférését a szerver adminisztratív vezérlésétől. Az alkalmazás megnyitása ne adjon Windows-parancsvégrehajtási jogosultságot.
- A jelenlegi LocalAccessFilter távoli/proxy blokkolását csak megvalósított, ellenőrzött hozzáférési modellre cseréld. Ne kapcsold ki pusztán a link működéséért.
- A Windows oldali végrehajtó csak előre meghatározott műveleteket fogadjon, minimális jogosultsággal, visszavonható hitelesítéssel. Ne legyen tetszőleges shell-parancs, fájlútvonal vagy Git-ref korlátlan átadása. Titkok a helyi biztonságos konfigurációban legyenek, ne repóban, naplóban vagy chatben.
- Ne nyiss nyilvános MySQL/imaging/admin/shell portot. A Windows–telefon tervhez Tailscale Serve és megbízható HTTPS használható, Funnel nélkül; az aktuális telepített súgót ellenőrizd. A ChatGPT vezérlési csatornáját ettől külön igazold.
- Önállóan futó, állandó távoli felügyeletet ne ígérj: az interaktív chatből kiadott műveletet, a helyi tartós szolgáltatást és az esetleges későbbi ütemezést egyértelműen különítsd el.

## Elfogadási próba

- Ebből a ChatGPT-környezetből lekérhető a valódi Windows tesztpéldány állapota és egy ártalmatlan, azonosítható tesztnapló.
- Innen indítunk egy leállított tesztpéldányt, megvárjuk a sikeres health ellenőrzést, ellenőrizzük a commitot, majd újraindítjuk és leállítjuk.
- Ismételt indítás, egyidejű művelet, kieső gép, lejárt/hibás hitelesítés és sikertelen build érthető, igazolt eredményt ad. Jogosulatlan vezérlés elutasítva.
- A telefonról megnyitható HTTPS-cím valós alkalmazást szolgál ki megfelelő jogosultsággal; telefonos megnyitás külön ellenőrzendő.
- Az asztali munka végén készített tesztösszefoglaló innen elérhető, de nincs állítás más beszélgetés automatikus teljes láthatóságáról.
- A személyes adatbázis/fotók/profil megmaradnak; a kezdeti próbák izolált tesztadatokon történnek.

## Mentés és átadás

Minden fejlesztést automatikusan commitolj/pusholj a dev ágra az AGENTS.md szerint. A teszt ágra csak valóban kipróbálható verzió kerülhet, pontos Windows-telepítési, csatlakozási, indítási és visszavonási útmutatóval. A végén add meg az ellenőrzött HTTPS-címet, a ténylegesen működő távoli műveleteket, a még helyi beavatkozást igénylő lépéseket és a teszteredményeket.
