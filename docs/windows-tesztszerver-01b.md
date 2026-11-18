# 01B – Átmeneti Windows-tesztszerver

2026-11-17. A felhasználó a Debian-telepítést későbbre tette: a Windows fejlesztőgép szolgál tesztszerverként, amíg be van kapcsolva és nem alszik. A megvalósítás alapja a main `bb06bd693209b5136e5f787bdd2ac9cd457cfc76` 01B/közös promptja és az új felhasználói pontosítás. Ez fejlesztési átadás, még nem igazolt élő szerverkapcsolat vagy tesztági kiadás.

## Mit ad ez a változat?

- Kizárólag a natív `.local/acceptance` tesztprofil indítása/állapota/leállítása/újraindítása. Web: `127.0.0.1:18740`, imaging: `127.0.0.1:18741`, MySQL: `127.0.0.1:43316`. A személyes profilt és a Docker-köteteket nem kezeli.
- A leállítás csak az azonosított teszt-JVM-eket állítja le, a MySQL-t, fotókat, vezérlőt és Tailscale-t megtartja. Windows `Stop-Process` történik: nem igazolt a JVM shutdown hookok kulturált lefutása. A személyes indító `-Stop` kapcsolóját távolról nem hívja.
- A sikeres helyi build után commitazonosító és SHA-256 hashpár; indítás előtt ellenőrzött JAR-ok és változatlan alkalmazásforrások szükségesek. A státusz a profilba másolt JAR-ok hashét közli; csak egyező buildjegyzéknél ad commitot. Ez helyi verziónyilvántartás, nem kriptográfiai buildtanúsítás vagy JVM-memóriaellenőrzés.
- Opcionális Tailscale Serve privát HTTPS a tesztweb előtt, Funnel nélkül. A tényleges DNS-nevet a helyi Tailscale adja. A már foglalt 443-as szolgáltatást nem írja felül. A felület és API egy originről nyílik, a 01A alkalmazásbelépéssel és Secure session cookie-val.
- Opcionális, külön privát GitHub-repót olvasó helyi vezérlő. Öt rögzített művelet; nincs shell, build, checkout, deploy vagy nyersnapló-export. A `diagnostics` jelenleg ugyanazt a szűrt health/verzióösszesítést adja, mint a `status`; részletes hibakivonat későbbi bővítés.

A Debian-migráció továbbra is terv; a Windows PowerShell-segédek nem Linuxos szerverindítók. Az ideiglenes gép későbbi cseréje más HTTPS origint és külön PWA-tárhelyet jelenthet, ezért adatköltöztetést kell tervezni.

## Első helyi indítás

A Windows-gépen, a friss `dev` munkapéldány gyökerében, normál PowerShellből. A meglévő natív Java/Maven/MySQL előfeltételek szükségesek, lásd README. A saját nem commitolt munkát őrizd meg; ne használj `reset --hard` parancsot. Ez a felhős munkamenet nem éri el közvetlenül a Windows-gépet.

```powershell
.\scripts\Build.ps1
.\scripts\TestServer.ps1 -Action start
.\scripts\Account.ps1 -TestRun -Action create -UserName david
.\scripts\TestServer.ps1 -Action status
```

A fiókeszköz helyben kéri a jelszót; nincs alapértelmezett jelszó. Meglévő tesztfióknál a create lépés kihagyható. A web `http://127.0.0.1:18740/leltar` címen próbálható. A tesztprofil kezdetben üres lehet; a személyes adatokat nem másoljuk át.

Ha a tesztprofil korábban Docker módban készült, a vezérlő `native_test_profile_required` hibával leáll: ne töröld a profilt, ne írd át vakon a runtime.txt-t. A meglévő tesztpéldány felmérése és tudatos migrációja külön helyi lépés. A meglévő személyes Docker-példány közben megmaradhat.

A `Build.ps1` csak sikeres Maven verify után készíti el a `.local/build-info.json` fájlt. Változtatott/nem commitolt alkalmazásforrásnál nem állít elő igazolt jegyzéket. `verified_build_required` vagy `source_changed_since_build` esetén a kívánt commitból ismét buildelj. Már más originnel vagy eltérő builddel futó tesztpéldánynál `restart_required` a válasz.

## Telefonos HTTPS-elérés

1. Telepítsd a [Tailscale Windows-klienst](https://tailscale.com/download/windows) a PC-re és az iOS-klienst a telefonra, jelentkezz be a saját tailnetbe. A MagicDNS/HTTPS előfeltételeket a Serve kérheti beállítani. Mindkét eszköznek legyen engedélyezett hozzáférése.
2. Windows Administrator terminálban, ugyanazzal a Windows-fiókkal, a projekt gyökerében:

   ```powershell
   .\scripts\Configure-TestHttps.ps1 -Enable
   ```

   A kapcsoló nélkül csak tervet mutat. A parancs ellenőrzi a meglévő Serve-konfigurációt, majd a saját, tényleges `https://…ts.net` címet írja ki és helyileg eltárolja. Nem nyit routerportot vagy nyilvános Funnel-t. Ha Funnel már engedélyezett, hibával megáll; azt a meglévő konfiguráció tulajdonosának kell rendeznie.
3. Normál PowerShellben:

   ```powershell
   .\scripts\TestServer.ps1 -Action restart
   ```

4. iPhone-on kapcsold be a Tailscale-t, Safariban nyisd meg a valóban kiírt címet `/leltar` útvonallal. Jelentkezz be a tesztfiókkal, hozz létre teszttárgyat fotóval, majd olvasd vissza Windowson. A session Secure cookie és CSRF ellenőrzése is az élő elfogadási próba része. A HTTPS bekapcsolása után ezt az origint használd a helyi böngészőben is; a loopback health továbbra is működik.

A tényleges Windows/Tailscale/iPhone próba még nincs elvégezve; jelenleg nincs ellenőrzött működő HTTPS-link. A teljes offline leltár és offline írás a 02/03 feladat, nem ennek a változatnak a képessége.

## Vezérlés ebből a beszélgetésből

A Tailscale önmagában nem köti ezt a ChatGPT-környezetet a gépedhez. A külön privát műveleti repóhoz a GitHub-csatlakozónak is hozzá kell férnie. A nyilvános `borosCode` repó erre nem használható; a vezérlő ellenőrzi a privát láthatóságot, és nyilvános repónál nem hajt végre semmit.

1. Hozz létre egy **privát** GitHub-repót (például `borosdavid/otthontar-ops`) inicializált `main` ággal, és engedélyezd hozzá a ChatGPT GitHub-csatlakozó hozzáférését. E munkamenet rendelkezésre álló csatlakozója nem tud új repót létrehozni. Csak te és a kifejezetten engedélyezett integrációk írhassák: ez a műveleti jogosultság tényleges határa.
2. Készíts lejáró fine-grained tokent kizárólag ehhez az egy repóhoz, `Contents: read/write` joggal. Ez a jog az egész repó tartalmára vonatkozik, nem csak a results mappára. Ne add meg chatben vagy parancssori argumentumban.
3. Normál Windows PowerShellben:

   ```powershell
   .\scripts\Configure-RemoteControl.ps1 -Repository borosdavid/otthontar-ops
   .\scripts\Run-RemoteControl.ps1 -Once
   ```

   A token rejtett helyi bekérésből, az aktuális Windows-fiók DPAPI-védelmével kerül a `.local/remote-control/token.dpapi` fájlba; a könyvtár ACL-je ugyanarra a felhasználóra korlátozott. A beállító helyi modulmásolatot telepít. A `-Once` ellenőrzi a privát repó elérését; **önmagában még nem bizonyít innen oda-vissza működő vezérlést**. A beállító nem validálja online a tokent. A vezérlőt emelt adminjoggal futtatni tilos.
4. Indítsd el folyamatosan ugyanebben a normál felhasználói terminálban:

   ```powershell
   .\scripts\Run-RemoteControl.ps1
   ```

   Kb. 30 másodpercenként lekérdez; Ctrl+C leállítja. Ez előtérben futó, kézzel indított ügynök, nem telepített Windows-szolgáltatás vagy automatikus indulás. A terminál bezárása, kijelentkezés, alvás vagy gépkikapcsolás megszünteti a vezérlést. A már futó alkalmazás a terminál bezárása után külön folyamatként megmaradhat.
5. Innen először `status` kérést kell beadni és azonosítóval a valódi választ kiolvasni. Csak a sikeres olvasási próba után engedélyezd helyben az alkalmazásműveleteket, az ügynököt előtte állítsd le:

   ```powershell
   .\scripts\Configure-RemoteControl.ps1 -Repository borosdavid/otthontar-ops -EnableMutations
   .\scripts\Run-RemoteControl.ps1
   ```

   A token változatlan marad; cseréjéhez `-ReplaceToken` kell. A kapcsoló nélkül újrafuttatott beállító visszaállítja a csak olvasó üzemet. A konfiguráció a helyi munkapéldányt kezeli; nincs Git-kérésből vezérelt önfrissítés. Megbízható helyi kódot futtat, az operációs rendszer felhasználójától nem izolált sandbox.

## A műveleti protokoll

Új kérelem a privát repó `main` ágán: `requests/<UUID-v4>.json`, legfeljebb 4096 UTF-8 bájt. Pontosan ezek a mezők; az időpontokat a beadáskor kell frissen kitölteni:

```json
{
  "schema_version": 1,
  "request_id": "<uj, kisbetus UUID-v4>",
  "server_id": "windows-test",
  "environment": "teszt",
  "operation": "status",
  "created_at": "<aktualis UTC ido Z vegzodessel>",
  "expires_at": "<created_at utan, legfeljebb 10 perccel kesobb>"
}
```

Ez csak sablon, így nem végrehajtható. Az engedélyezett operation: `status`, `diagnostics`, `start`, `stop`, `restart`. A server_id a helyi beállításból jön; nem adható át útvonal vagy shellargumentum. Nincs külön `protocol-version` fájlra támaszkodó önfrissítés: a séma 1-es verzióját a helyi kód rögzíti.

Az ügynök minden kör elején rögzíti a main commitot, és ugyanabból a commitból olvassa a kérelemjegyzéket és tartalmat. Az aktív sor legfeljebb 100 fájlt tartalmazhat, az ügynök körönként legfeljebb 5 új kérést dolgoz fel. A lezárt kérés/eredmény párokat szükség szerint kézzel archiváld. Ha egy korábban feldolgozott kérést átírnak, az ügynök `request_changed` figyelmeztetést ad a helyi terminálon, megőrzi az eredeti eredményt, és folytatja a többi kérés feldolgozását.

Válasz: `results/<azonos UUID>.json`. Csak az állapot/hibakód, UTC idő, három technikai állapotjelző, commit és JAR-hashek engedélyezettek; nincs jelszó, token, cookie, napló, fájlútvonal vagy tárgyadat. `database_tcp_open` csak portellenőrzés, nem SQL-integritási próba. A diagnostics nem ad szabad lemezterületet vagy nyers hibaszöveget.

A tartós helyi napló **a végrehajtás előtt** `executing` állapotot ír. Végállapot: `succeeded`, `failed`, `unknown` vagy `rejected`. Hálózati eredményírási hiba után csak a publikálást próbálja újra. Megszakadt `executing` állapotból `unknown/interrupted_do_not_replay` lesz, automatikus ismétlés nélkül. A bizonytalan művelet után friss status-kérés kell; nincs elosztott „pontosan egyszer” garancia.

Az indító alfolyamat 150 másodperces határideje után `unknown/start_timeout_unknown` a válasz. Az alfolyamatot nem öli meg vakon; a helyi PID+indulásidő jelző újabb módosító műveletet tilt, amíg ugyanaz az indító fut. A státusz továbbra is kérhető. A még élő, de health nélküli korábbi JVM mellett az indítás szintén `restart_required` hibával áll meg, párhuzamos példány helyett. Ne indíts közben külön `Start.ps1` példányt; a régi indító nem használja a vezérlő lockját. Fájlzár védi a vezérlő párhuzamos kéréseit.

Egy beadott kérés még csak sorba állt. Ha a lejáratig és az ismert legfeljebb 150 másodperces indítási időn túl sincs válasz, az állapot nem igazolt; ne jelents sikert vagy kikapcsolt gépet pusztán a válaszhiányból. A tokent visszavonni, az ügynököt leállítani és a tailnet-hozzáférést törölni külön művelet. A lejárati ellenőrzéshez pontos Windows-óra szükséges.

## Ellenőrzések és hátralévő próbák

A 2026-11-18-i javítás után a `restart` előbb ellenőrzi a buildet és a HTTPS-beállítást. Ha hibát talál, a futó példányt meghagyja. A bővített PowerShell-teszt 77 ellenőrzést futtat, köztük a hibás konfigurációjú újraindítást és az átírt kérést követő érvényes kérés kezelését. A korábban telepített ügynöknél a javított modulokat leállítás után a `Configure-RemoteControl.ps1` ismételt futtatása telepíti. Add meg a meglévő privát repót, szerverazonosítót és a használni kívánt módosítási engedélyt.

Ebben a Linux munkakörnyezetben PowerShell 7.5.13 alatt **71 sikeres assertion**: az összes PowerShell-fájl szintaktikai elemzése; szigorú séma/cél/lejárat/művelet; fájlzár; valódi Git/buildjegyzék és módosított JAR/forrás felismerése; ismétlés/módosítás; tartós napló és megszakadás; eredményírási hiba; titokszűrés; csak olvasó alapmód; mock GitHub-poll és írásütközés; HTTPS-idempotencia/Funnel/foglalt port; idegen folyamat elleni védelem; mock indítás/leállítás és személyesprofil-megőrzés.

```powershell
pwsh -NoProfile -File .\scripts\tests\Remote-Control.Tests.ps1
```

A mockok nem működő Windows-szerver bizonyítékai. A Windows PowerShell 5.1-et célozza a kód, de az ottani tényleges végrehajtás, DPAPI/ACL, CIM-folyamatazonosítás, Java/MySQL-indítás, valódi GitHub-privátrepó és iPhone/Tailscale HTTPS még helyi próbát igényel. Java/alkalmazáskód és migráció e fázisban nem változott; a korábbi 01A-tesztek eredménye a [01A-riportban](fiokok-01a.md), most nem futottak újra.

Élő elfogadás: csak olvasó kérés innen valódi válasszal → start/health/verzió → ismételt start → restart/health → stop; személyes adatok változatlanok; tokenvisszavonás/lejárat; gépalvás/hálózati kiesés; HTTPS-belépés és fényképes teszttárgy iPhone-ról. A 01A valódi V3-migrációs és fiókkezelési próbái is szükségesek. Ezek lezárásáig a 01B aktív, `Fejlesztés alatt`, a `teszt` ág és címkéi változatlanok.

A gép energiaellátását/alvási házirendjét nem módosítja a kód. A helyi mentést és a későbbi Debianra költöztetést külön kell elvégezni. HTTPS visszavonásakor a saját konfiguráció ellenőrzése után `tailscale serve --https=443 off` kapcsolhatja le ezt a portot; ne használj globális Serve resetet más szolgáltatások mellett. Az alkalmazás belépési originjét helyben kell visszaállítani és újraindítani.

## Hivatalos források

- [Tailscale Serve parancs és háttérmód](https://tailscale.com/docs/reference/tailscale-cli/serve)
- [Windows Serve példák / adminterminál](https://tailscale.com/docs/reference/examples/serve)
- [Microsoft ConvertFrom-SecureString / DPAPI](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.security/convertfrom-securestring)
- [GitHub Contents API](https://docs.github.com/en/rest/repos/contents)
