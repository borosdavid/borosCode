# Közös távoli szerverhozzáférés

**Frissítve:** 2026-11-24. A jelenlegi alkalmazáspéldány Ubuntu alatt érhető el. A privát iPhone-os üzemeltetést a felhasználó iSH-val és Tailscale-lel végzi.

## Jelenlegi megoldás és jogosultságok

| Szerep | Kapcsolat | Jogosultság |
|---|---|---|
| Alkalmazásfelhasználó | `https://boroscode.com/otthontar/` → Cloudflare Worker/Tunnel → Ubuntu, alkalmazásfiókos belépéssel | A közös leltár és az alkalmazás funkciói |
| Dávid mint gépüzemeltető | iPhone → iSH → SSH a Tailscale privát hálózatán → Ubuntu | Az Ubuntu-fiókhoz rendelt üzemeltetési jogok |

A telefonról indított frissítés és a verzióellenőrzés a [dev Ubuntu útmutatójában](https://github.com/borosdavid/borosCode/blob/ffeecbe0e4745b99be76e194a85e80190971f3e2/docs/KNOWHOW-ubuntu-frissites.md) található. Az alkalmazásfiók kizárólag az Otthontár funkcióira jogosít. A privát SSH-hozzáférést a felhasználó a saját eszközéről használja.

## 01B – Külön Windows-tesztkörnyezet

Az alábbi útvonalak és elfogadási próbák a D-011 szerint előkészített, elkülönített Windowshoz tartoznak. A natív helyi kezelés a Windows-fiók és a PowerShell-segédek feladata. A ChatGPT-műveleti csatorna terve a külön privát GitHub-repót olvasó Windows-ügynök, öt rögzített művelettel. Az élő műveleti kapcsolat még igazolásra vár.

### Elkészült fejlesztés

A dev [`cf191426810ded4ef4b362d56c35735165321cb4`](https://github.com/borosdavid/borosCode/commit/cf191426810ded4ef4b362d56c35735165321cb4) tartalmazza:

- `TestServer.ps1`: rögzített `.local/acceptance`, 18740/18741/43316, kizárólag natív mód. A leállítás csak azonosított teszt-JVM-eket kezel; MySQL és személyes adatok megmaradnak. A Stop-Process nem igazolt kulturált JVM-leállítás.
- `Write-BuildInfo.ps1` és a sikeres Build.ps1 végén jegyzék: commit, inventory/imaging SHA-256, változatlan alkalmazásforrások ellenőrzése. Nincs Git-alapú önfrissítés vagy távoli build/deploy.
- `Configure-TestHttps.ps1`: Tailscale Serve a `127.0.0.1:18740` előtt, a tényleges helyi DNS-névvel. Funnel elutasítása, meglévő 443 szolgáltatás megőrzése. Nem nyit publikus routerportot.
- `Configure-RemoteControl.ps1` / `Run-RemoteControl.ps1`: külön privát GitHub-repóra rögzített ügynök; normál Windows-felhasználó, DPAPI+ACL tokenvédelem, csak olvasó alapmód. Kézi indítású előtérfolyamat, 30 másodperces lekérdezési szünettel; nincs Windows-szolgáltatás vagy boot-feladat telepítve.

A `status` és `diagnostics` engedélyezett health/verzióösszesítés, nyersnapló nélkül. `start`, `stop`, `restart` csak helyi módosításengedély után. A diagnosztika nem SQL-integritási teszt: a DB-jelző TCP-portellenőrzés. A hashek a profilba másolt JAR-okat azonosítják, nem a JVM memóriáját tanúsítják.

## Miért kell külön vezérlőrepó?

A ChatGPT-környezet nem lesz automatikusan tailnet-tag, és azonos ChatGPT-fiókkal sem lát másik asztali munkamenetbe. Az elérhető GitHub-csatlakozó fájl-/commitírásra és -olvasásra alkalmas; új workflow_dispatch indítására és repo létrehozására ebben a munkamenetben nincs eszköz.

A `borosdavid/borosCode` alkalmazásrepo nyilvános. Műveleti kérést/eredményt, szervernaplót vagy tokent oda nem teszünk. Külön privát repo szükséges, például `borosdavid/otthontar-ops`; a név javaslat, a repo nincs igazoltan létrehozva/csatlakoztatva. Az ügynök minden körben ellenőrzi a privát jelleget, és kérésből nem enged repo-/branch-/gépcélváltást.

## Protokoll és hibakezelés

- `requests/<UUID-v4>.json`, pontosan hét mező: schema_version=1, request_id, server_id, environment=teszt, operation, created_at, expires_at. UTC Z idő, legfeljebb 10 perces élettartam, 4096 UTF-8 bájt. Nincs shell, útvonal, URL, titok vagy szabad argumentum.
- A kérésjegyzék és a tartalom ugyanabból a rögzített main commitból olvasódik. Maximum 100 aktív kérésfájl, körönként 5 új feldolgozás; archiválás kézi.
- Tartós helyi napló és fájlzár. Végrehajtás előtt executing, utána succeeded/failed/unknown/rejected. Az executing állapot megszakadása unknown; sem ismételt kérés, sem eredményírási ütközés nem futtatja újra a műveletet.
- `results/<UUID>.json`: csak engedélyezett bool health-jelzők, commit/hash, időpont és rögzített hibakód. Nincs nyersnapló, tárgyadat, jelszó, cookie vagy token. A privát repo sem titoktároló.
- Start-alfolyamatra 150 másodperces határ; utána unknown, vak folyamatfaölés nélkül. PID+indulásidő jelző tiltja az újabb módosítást, míg ugyanaz az indító él. Élő, de health nélküli JVM mellett start helyett explicit restart szükséges. A régi helyi Start.ps1-et ne futtassuk párhuzamosan a vezérlővel.
- A bizalmi határ a privát repo tényleges írási joga. A helyi fine-grained token csak az egy ops repó Contents read/write jogát kapja; ez repo-szintű jog, nem csak a results mappára korlátozott. A helyi Windows-fiókhoz tartozó kódot az ügynök nem sandboxolja.

A kérelem commitolása csak sorba állítás. Sikerhez az azonosítóhoz tartozó valós eredményt kell visszaolvasni. Válaszhiányból nem következik sem siker, sem kikapcsolt gép. A folyamatos helyi ügynök nem jelent folyamatos ChatGPT-háttérmunkát.

## Aktiválás és elfogadás

A konkrét parancsok, séma, leállítás/visszavonás és tesztlista a [verzióhoz kötött Windows-útmutatóban](https://github.com/borosdavid/borosCode/blob/cf191426810ded4ef4b362d56c35735165321cb4/docs/windows-tesztszerver-01b.md).

1. A Windows munkapéldány friss dev kódjából helyi build/tesztindítás; külön tesztfiók és valódi MySQL/fiókpróba.
2. Tailscale-bejelentkezés Windows/iPhone eszközökön, HTTPS-konfigurálás és alkalmazás-újraindítás; tényleges telefonos belépés/fotópróba. Nincs előre kitalált HTTPS-cím.
3. Privát ops repo létrehozása, GitHub-csatlakozó jogosultsága, helyi DPAPI-tokenbekérés és normál felhasználói ügynökindítás, először csak olvasó módban.
4. Innen status-kérés valós válasszal, majd helyi módosításengedély után start/health/verzió/restart/stop. Személyes profil ellenőrzése változatlanságra.
5. Lejárat, jogosultság-visszavonás, hálózati kiesés, ismétlés/ütközés/timeout élő próbája.

**Igazolt:** 77 automatizált PowerShell-ellenőrzés, köztük szimulált GitHub/HTTPS és folyamatkezelés. A [CI-futás](https://github.com/borosdavid/borosCode/actions/runs/44285364279) Windows PowerShell 5.1 alatt is átment. **Még igazolandó a saját Windows-gépen:** DPAPI/ACL/CIM/Java/MySQL, a privát repóhoz kapcsolt élő vezérlés és a Windows-os Tailscale HTTPS eszközpróbája. Ezért a 01B Fejlesztés alatt, nem átadott tesztkiadás.

## Üzem és későbbi Debian

A Windows kikapcsolása/alvása megszünteti az online elérést; az ügynök terminálját nyitva kell tartani. A kód nem változtat energiasémát, tűzfalat, routert vagy rendszerindítást. A tesztalkalmazás leállítása nem kapcsolja ki a gépet vagy a vezérlőt. A token, tailnet-eszköz és alkalmazásfiók külön visszavonható.

A dedikált Debian-szerver, kulcsos SSH, systemd/Compose alapú indulás, kulturált leállítás, külső mentés és ellenőrzött visszaállítás későbbi feladat marad. A mostani Windows natív PowerShell-indítót nem nevezzük Debian-megoldásnak. A migrációs HTTPS-origin és a PWA helyi tárhelyének átvezetése külön figyelmet igényel.

## Hivatalos háttérforrások

- [Tailscale Serve](https://tailscale.com/docs/reference/tailscale-cli/serve)
- [Windows Serve példák](https://tailscale.com/docs/reference/examples/serve)
- [Microsoft DPAPI / ConvertFrom-SecureString](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.security/convertfrom-securestring)
- [GitHub repository contents API](https://docs.github.com/en/rest/repos/contents)

A GitHub-műveletsor saját, korlátozott protokoll, nem kész GitHub/OpenAI távoli adminisztrációs szolgáltatás. Az élő kapcsolat csak a fenti végponttól végpontig tartó próbával igazolható.
