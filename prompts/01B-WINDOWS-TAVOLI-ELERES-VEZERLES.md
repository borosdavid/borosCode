# 01B – Átmeneti Windows-tesztszerver és távoli vezérlés

> **Státusz:** Fejlesztés alatt
> **Pontosítás / akadály:** Ez a D-011 szerint előkészített külön Windows-tesztkörnyezet. Az élő Windows/Tailscale/privát GitHub-kapcsolat még igazolásra vár. A használatban lévő webes útvonal az Ubuntu-példányhoz vezet Cloudflare-en, a telefonos adminisztráció iSH és Tailscale feletti SSH.
> **Megvalósítás:** dev [`f3c2f74`](https://github.com/borosdavid/borosCode/commit/f3c2f742fd57b4633a226f880d74685984549d1c), az alapimplementáció: [cf19142](https://github.com/borosdavid/borosCode/commit/cf191426810ded4ef4b362d56c35735165321cb4).
> **Ellenőrzés:** 77 ellenőrzés sikeres PowerShell 7.5.13/Linux alatt, köztük a hibás build előtti leállítás megelőzése és a sor folytatása egy átírt kérés után. A GitHub futtatóján a kódolási javítás után Windows PowerShell 5.1 alatt is sikeres a 77 ellenőrzés, és az Edge PWA-próba is átment. A helyi folyamatkezelés és az alkalmazás privát GitHub-vezérlése a PowerShell-próbában szimulált. [CI futás](https://github.com/borosdavid/borosCode/actions/runs/44285364279).
> **Felhasználói teszt:** Nincs átadva; A tényleges gépen a DPAPI/ACL, natív MySQL/Java, élő privát GitHub-vezérlés és iPhone HTTPS hátravan. [Eszközpróbák](https://github.com/borosdavid/borosCode/blob/16ad22ba72ed86d0a806f5f6a58d2b851fdf2af3/docs/windows-tesztszerver-01b.md#ellenőrzések-és-hátralévő-próbák).
> **Frissítve:** 2026-11-24. [Státuszszabályok](../projekt/STATUSZOK.md).

## Cél és elfogadott változás

A felhasználó 2026-11-17-i utasítása alapján készült egy elkülönített Windows-tesztszerver a fejlesztőgéphez, amely bekapcsolt állapotban használható. A Debian-laptop dedikált szerepe megmarad hosszú távú tervként; a mostani feladatnak nem előfeltétele a laptop hardverfelmérése vagy telepítése. A Windows elalvása/kikapcsolása az online elérést megszünteti; ébresztés és folyamatos üzem külön feladat.

A [közös promptszabályok](00-KOZOS-UTASITASOK.md), [munkarend](../AGENTS.md), [D-011 döntés](../projekt/DONTESI-NAPLO.md) és [aktuális hozzáférési terv](../projekt/SZERVER-TAVOLI-HOZZAFERES.md) követendő. Más asztali beszélgetés belső állapotát nem látjuk automatikusan; commitok és riportok jelentik a közös ellenőrzési pontot.

## Első megvalósítási kör

1. A natív Windows `acceptance` tesztprofil rögzített kezelése: status/diagnostics/start/stop/restart. Személyes adatbázist, fotókat, Docker-köteteket ne érintsen; adatmásolás vagy profilváltás ne legyen automatikus.
2. A helyi build sikeres tesztje után commit és JAR-hashek rögzítése, indulás előtt forrás/build ellenőrzése. Ne töltsön le és futtasson Gitből kapott kódot, shellt vagy tetszőleges refet.
3. Kölcsönös kizárás, tartós műveleti napló, UUID, rövid lejárat és sémaellenőrzés. Végrehajtás előtt tartós executing jelölés, utána succeeded/failed/unknown/rejected. Bizonytalan műveletet ne ismételjen vakon; az eredmény publikálása külön újrapróbálható.
4. A 01A session/belépési védelemre épülő privát HTTPS Tailscale Serve segítségével, csak a loopback tesztwebre. Nincs Funnel, routerport-nyitás, publikus MySQL/imaging/admin-végpont. Valódi DNS-nevet használjon; meglévő Serve-konfigurációt ne írjon felül.
5. Külön privát GitHub-repót olvasó, helyben telepített ügynök a ChatGPT-kérésekhez; a nyilvános alkalmazásrepóba ne kerüljenek műveleti kérések/eredmények. Token helyben, Windows-fiókhoz kötött DPAPI-val és ACL-lel; kezdetben csak olvasó mód, később helyi explicit módosításengedély. Az ügynök normál felhasználóként fusson.
6. Pontos indítási, csatlakozási, hibakeresési és visszavonási leírás a dev kód mellett. Az itteni felhős környezet nem azonos a Windows-géppel: csak a valódi kapcsolatból származó eredmény bizonyít távoli vezérlést.

A dev implementáció az alkalmazás-JVM-eket Windows Stop-Process segítségével állítja le; a MySQL-t futva hagyja. Ez nem igazolt graceful shutdown. A diagnostics egyelőre a health/verzióösszesítést adja, nyersnapló-export nélkül. Az ügynök kézzel indított előtérbeli folyamat, nem automatikusan telepített Windows-szolgáltatás.

## Helyi aktiválás és még szükséges próbák

A részletes parancsok és korlátok: [Windows 01B útmutató a megvalósító commitban](https://github.com/borosdavid/borosCode/blob/cf191426810ded4ef4b362d56c35735165321cb4/docs/windows-tesztszerver-01b.md).

- A friss dev munkapéldányból helyi Build.ps1, TestServer start, tesztfiók létrehozása és MySQL V3/fiókpróba; személyes adatok megőrzése.
- Tailscale Windows/iPhone bejelentkezés és a gép valós HTTPS-nevének beállítása; belépés/fotós tárgyművelet mindkét eszközről. A hálózati VPN nem helyettesíti az alkalmazásbelépést.
- Külön privát műveleti repo létrehozása és GitHub-csatlakozóhoz rendelése. E környezetben új repo létrehozására nincs eszköz; token és első Windows-indítás helyben szükséges.
- Először innen beadott, egyedi status-kérésre a valódi gép válasza; csak azután helyi módosításengedély és start → health/verzió → ismételt start → restart → health → stop próba.
- Hibás/lejárt/átírt/ismételt kérések, párhuzamosság, GitHub-írásütközés, elvesző kapcsolat, folyamatindítási timeout és tokenvisszavonás élő próbája. Siker helyett ismeretlen eredményt kell jelezni, ha nincs visszaigazolás.

Kikapcsolt gépnél, hiányzó tailnet-/repo-hozzáférésnél nincs kész szerverkapcsolat. Ne adj kitalált működő URL-t, és ne jelents teljes 01B-lezárást pusztán a mockokból.

## Későbbi bővítések

A teljes 01B körben hátravan a kulturált leállítás igazolása, előre engedélyezett szűrt hibakivonat, ellenőrzött build/teszt riportok megosztása, szükség szerint automatizált indulás és külön telepítési folyamat. Ezek nem jelentenek általános shell-jogot. A Debian-migrációnál újra igazolandó a szolgáltatáskezelés, tartós tárolás, külső mentés/visszaállítás és az állandó privát HTTPS origin; a mostani natív PowerShell-indító nem vihető át változtatás nélkül Linuxra.

A 01C webes felület és az Ubuntu/Cloudflare útvonalra épülő 02–04 iPhone-fázisok ettől a Windows-próbától függetlenül haladhatnak. A 01B a hiányzó élő elfogadási feltételek miatt aktív marad, nem kerül a kesz mappába. Minden fejlesztés dev-be, a tervek külön mainbe; tesztági kiadás csak a munkarendi feltételek teljesülésekor, kitöltött átadási lappal és címkével.
