# Közös távoli szerverhozzáférés – konkrét terv

**Állapot:** kidolgozott javaslat, 2026-11-17; nincs telepítve vagy élő kapcsolattal igazolva. A gépszerepeket D-007, az alkalmazásbelépést D-008 rögzíti a [döntési naplóban](DONTESI-NAPLO.md).

## Három külön jogosultság

| Szerep | Kapcsolat | Jogosultság |
|---|---|---|
| Alkalmazásfelhasználó | Tailscale + privát HTTPS + alkalmazásbeli felhasználónév/jelszó | Minden alkalmazásmodul és közös adat, azonos jogosultsággal |
| Dávid mint gépüzemeltető | Tailscale + saját SSH-kulcs, Windows/iPhone SSH-kliens | Szerveradminisztráció saját Linux-fiókkal és szükség szerinti sudo-val |
| ChatGPT-munkamenet | GitHub-csatlakozó → privát műveleti repó → kifelé kapcsolódó szerverügynök | Csak engedélyezett állapotlekérés, diagnosztika és alkalmazásvezérlés |

A ChatGPT nem kap közös emberi jelszót, saját alkalmazásfiókot vagy általános root SSH-kulcsot. Az alkalmazásban azonos jogosultságok nem jelentenek azonos szerverüzemeltetési jogot.

## A felhasználó közvetlen hozzáférése

- Tailscale a Debian-laptopon, Windowson és iPhone-on; az eszközökhöz szűk hozzáférési szabályok.
- A PWA a Debianon futó Tailscale Serve mögött, állandó privát HTTPS originről nyílik. A konkrét hostnevet a telepítés adja, itt nincs kitalált működő cím.
- A Debian OpenSSH-szolgáltatása csak a kijelölt privát elérési útvonalon legyen használható, a tűzfalon és a Tailscale-szabályokon is ellenőrizve. Windows és iPhone külön, visszavonható SSH-kulcsot kap; root SSH-belépés és jelszavas SSH helyett saját adminfiók.
- iPhone-ról egy SSH-kliens kell a közvetlen konzolhoz. Az alkalmazás megnyitásához Safari/PWA és Tailscale elég; a két feladat külön marad.
- A routeren nem kell nyilvános adminisztrációs portot továbbítani. A helyi első konfiguráláskor ténylegesen próbáljuk ki az elérhetőséget és a kizáródás elkerülését.

## Miért nem pusztán Tailscale vagy GitHub Actions?

Az itteni ChatGPT-futtatókörnyezet nem automatikusan tagja a felhasználó tailnetjének. Az elérhető GitHub-csatlakozó fájl-/commitírást és -olvasást támogat; új `workflow_dispatch` indításához jelenleg nincs közvetlen eszköz. Workflow-újrafuttatás nem megfelelő új parancsok továbbítására.

A `borosdavid/borosCode` repo a 2026-11-17-i GitHub-lekérdezés szerint **nyilvános**. Személyes szerverre közvetlenül kötött általános self-hosted Actions runner helyett szűk műveletsort fogyasztó ügynököt javaslunk. Az ügynök sem branchből letöltött scriptet, sem PR-kódot nem futtat.

## ChatGPT → GitHub → Debian vezérlési csatorna

Külön **privát vezérlőrepó** szükséges, javasolt név `borosdavid/otthontar-ops`. Ez még nincs létrehozva vagy csatlakoztatva. A meglévő alkalmazásrepo láthatóságát nem változtatjuk meg.

A vezérlőrepóban:

- `requests/<uuid>.json`: egy változtathatatlan, rövid életű műveleti kérelem.
- `results/<uuid>.json`: ugyanahhoz az azonosítóhoz tartozó, méretkorlátos, szűrt technikai eredmény.
- `protocol-version`: a megengedett adatséma verziója; a fájl önmagában nem telepít új protokollt a szerverre.

A szerveren telepített systemd-szolgáltatás csak kifelé, HTTPS-en kérdezi le a privát repót, kezdetben például 30 másodpercenként. Sikeresen kiépített rendszerben a kérelem rendszerint a következő lekérdezéskor felvehető; a végrehajtás időtartama ettől különbözik. GitHub- vagy internetkieséskor új kérést nem hajt végre; a futó alkalmazás ettől működik tovább.

Folyamat:

1. A felhasználó itt kér egy engedélyezett műveletet.
2. A ChatGPT egyedi azonosítóval, célkörnyezettel és lejárattal kérelmet commitol a **privát** vezérlőrepóba.
3. A szerver ellenőrzi a repót, ágat, tényleges GitHub-hozzáférési határt, szerverazonosítót, szigorú sémát, lejáratot, ismétlést és a saját helyi műveletengedélyeit.
4. A helyileg telepített, root tulajdonú vezérlő egy rögzített műveletet végez, majd health/állapotellenőrzést futtat.
5. Az ügynök szűrt eredményt ír vissza; a ChatGPT ugyanazt az azonosítót olvassa és az eredményt közli.

A kérés feltöltése csak „sorba állítva”, nem „végrehajtva”. Nincs folyamatos ChatGPT-háttérmunka; az ügynök viszont önálló helyi szolgáltatásként működik.

## Első változat műveletei

| Művelet | Tartalom | Korlát |
|---|---|---|
| status | inventory/imaging/adatbázis futása, health és kiadásazonosító | Nem kérdez le személyes leltárt |
| diagnostics | Rögzített hibakódok, health, szabad tárhely és engedélyezett technikai összesítés | Nincs nyers alkalmazásnapló vagy tetszőleges fájlolvasás |
| start | A kijelölt alkalmazáskörnyezet indítása | A már futó példányt nem duplikálja |
| stop | A kijelölt alkalmazáskörnyezet rendezett leállítása | Nem állítja le a gépet, Tailscale-t, SSH-t vagy a vezérlőügynököt |
| restart | A kijelölt alkalmazáskörnyezet újraindítása | Megszakított műveletet nem ismétel vakon |

Első aktiváláskor csak az izolált **teszt** környezet vezérelhető. Az éles személyes környezetet Dávid később, kifejezetten helyi engedélyezéssel vonhatja be. A tesztparancs ne érhessen el közös adatbázist vagy személyes képkötetet.

A teljes naplót Dávid SSH-val helyben olvassa. AI-elemzéshez csak előre engedélyezett, szükséges és szűrt kivonat kerül a privát csatornára. A Git-előzmény tartós: a privát repo sem titoktároló, jelszó/token/cookie vagy személyes tárgyadat ide sem írható.

Telepítés, upgrade, mentés-visszaállítás, adatbázis-migráció, gépújraindítás és tetszőleges shell **nem része az első csatornának**. Ellenőrzött kiadások telepítése későbbi bővítés: helyileg jóváhagyott, változatlan release-azonosítóval és rollback-tervvel; nem tetszőleges Git-refből futtatott kóddal.

## Protokoll és végrehajtási határok

Példa szerkezet, nem végrehajtandó kérelem:

```json
{
  "schema_version": 1,
  "request_id": "<uuid>",
  "server_id": "<helyileg-engedelyezett-azonosito>",
  "environment": "teszt",
  "operation": "status",
  "created_at": "<UTC ISO-8601>",
  "expires_at": "<UTC ISO-8601, legfeljebb 10 perc mulva>"
}
```

- Nincs parancssor, script, URL, útvonal, jelszó, változólista vagy szabad szöveges shellargumentum. Ismeretlen mező/érték elutasítandó; méretkorlát például 4 KiB.
- A JSON-ban állított felhasználónév nem hitelesítés. A bizalmi határ a privát repóhoz való tényleges GitHub-írási hozzáférés. Csak a tulajdonos és az általa engedélyezett integráció írhat; ha további írók vannak, aktiválás előtt újra kell értékelni a modellt.
- A repó és branch nem váltható át kérésből. Az ügynök kódja és privilegizált segédprogramja nincs automatikusan frissítve a vezérlőrepóból.
- Az ügynök külön Linux-fiókkal fut; nincs általános sudo, Docker-csoporttagság vagy Docker socket hozzáférés. Csak root tulajdonú, szűken engedélyezett segédprogramot hívhat, amely rögzített környezetekre és műveletekre validál. A Compose- és környezeti fájlokat az ügynök nem módosíthatja.
- A szerveren tárolt külön GitHub App/fine-grained hozzáférés csak a privát vezérlőrepóhoz kapjon szükséges tartalomjogot, lejárattal és visszavonhatósággal. Ennek repo-szintű írási joga nem pusztán eredményfájlokra korlátozott; ezt a korlátot dokumentálni kell. Token csak helyi védett konfigurációban legyen.
- UUID-alapú helyi tartós feldolgozási napló, lejárati és óraellenőrzés, kölcsönös kizárás, műveleti időkorlát és bounded eredményméret kell.
- Az állapotátmenetek: received → executing → succeeded/failed/unknown/rejected. A végrehajtás előtti tartós executing jelölés és a gépleállás miatti bizonytalan eredmény kezelendő; nem ígérünk elosztott „pontosan egyszer” végrehajtást. A bizonytalan restart helyett állapotot kérdezünk.
- Ütköző GitHub-fájlírásnál friss SHA-val szabályos újrapróbálás; nincs force push. A feldolgozott kérelem utólagos átírása nem új művelet.
- A visszavonás módja: helyi ügynök leállítása, GitHub-hozzáférés visszavonása és tailnet-eszköz/kulcs külön visszavonása. A közvetlen SSH-vészút az ügynöktől független marad.

## Telepítési lépések és elfogadás

1. Laptop hardver és Debian állapot felmérése; első helyi telepítés és kulcsos SSH/Tailscale kapcsolat.
2. A külön privát vezérlőrepó létrehozása a felhasználó fiókjában, a GitHub-csatlakozó hozzáférésének beállítása. A repo létezése, privát jellege és innen történő írás/olvasás ténylegesen ellenőrzendő.
3. Ügynök, szűk segédprogram, elkülönített tesztpéldány és helyi tokenbeállítás. Előbb szimulált, majd csak olvasó élő próba.
4. Innen beadott status-kérelemre a valódi Debian-gép válaszol, azonosítóval és ellenőrizhető kiadásinformációval.
5. Izolált tesztpéldány start → health → restart → health → stop próbája, változatlan személyes adatokkal.
6. Hibás/lejárt/ismételt kérelmek, ismeretlen művelet, párhuzamosság, áramszünet utáni unknown, GitHub-kiesés és jogosultság-visszavonás próbája.
7. Alkalmazásfiókok és a proxyhoz illő szerveroldali védelem elkészülte után védett telefonos HTTPS-próba. A hálózati hozzáférés nem helyettesíti az alkalmazásbelépést.

**Most hiányzik:** laptop hardveradata és telepítése, a privát vezérlőrepó és jogosultsága, valamint az ügynök implementációja/éles próbája. Ez a dokumentum tervezési eredmény; élő szerverhozzáférést nem hoz létre.

## Ellenőrzött háttérforrások

- [Tailscale SSH feletti normál SSH](https://tailscale.com/docs/reference/ssh-over-tailscale)
- [Tailscale Serve](https://tailscale.com/docs/features/tailscale-serve)
- [GitHub self-hosted runner biztonság](https://docs.github.com/en/actions/reference/security/secure-use)
- [GitHub repository contents REST API](https://docs.github.com/en/rest/repos/contents)

A GitHub-alapú, szűk műveletsor saját rendszerterv; nem kész GitHub/OpenAI távoli adminisztrációs szolgáltatás. A fájlírási képességet a projekt korábbi commitjai igazolják; a külön privát repo elérését a telepítéskor még ellenőrizni kell.
