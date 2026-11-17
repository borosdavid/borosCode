# 01 – PWA és mobilos alapok

## Állapot és kiindulás

A munka a `ce9fdd6` GitHub-állapotból készült. A tegnapi JPA-átállás itt még nem volt elérhető; a Ledger, Photos, Web, inventory/pom.xml és a Flyway migrációk változatlanok. A perzisztencia-refaktorral a közös felületi módosításokat és az új hozzáférési szűrőt össze kell fésülni. Az imaging modulban kizárólag a JPEG-előnézet EXIF-tájolása bővült, az eredeti JPEG bájtjai megmaradnak.

Audit: a felület Thymeleaf + önálló JavaScript, relatív `/api` útvonalakkal. Nincs app-login, session vagy többfelhasználós jogosultság. A natív webkiszolgáló és a Docker publikált port alapból loopbacken érhető el. A kamera/Service Worker telefonon megbízható HTTPS-t igényel; az iPhone-on a `127.0.0.1` a telefont jelenti. A korábbi JPEG-választó nem adott kameraopciót; az előnézet nem kezelte az EXIF-tájolást.

**A helyi fejlesztési alap elkészült. A távoli éles használat és a valódi iPhone-elfogadás még nincs engedélyezve/igazolva.** Ez nem a teljes offline leltár: a 02-es fázisig az adatlista online marad.

## Elkészült működés

- Magyar Otthontár manifest, 192/512 px ikonok, külön 180 px Apple touch ikon, iOS meta, safe area, legalább 44 px gombok és 16 px űrlapmezők. Indulási URL `/leltar`, scope `/leltar`, minden erőforrás és API same-origin.
- A gyökér `/sw.js` a `/leltar` scope alatt működik. Csak a statikus, személyes adat nélküli alapfájlok és az offline tájékoztató cache-elődnek. A navigáció hálózati választ használ, hálózati hiba esetén a tájékoztató nyílik meg. API, fotó, POST és hitelesítési válasz nincs a cache-ben. Élő HTML-t nem ment.
- A verzióváltás telepítése atomikus `cache.addAll`; sikertelen frissítésnél a régi worker működik tovább. Nincs `skipWaiting` vagy automatikus újratöltés. A frissítés az összes nyitott lap bezárása után aktiválódik. Csak a saját régi shell-cache törlődik; IndexedDB és más cache nem. **Statikus fájl módosításakor a sw.js VERSION értékét is emeld.**
- Külön Fotók kiválasztása és Fotó készítése. A `capture` eszközpreferencia: a tényleges választót a Safari határozza meg. Nem kérünk kötelező kameraengedélyt; elutasítás után a fotótár használható.
- JPEG eredeti bájtok megőrzése; más, böngésző által dekódolható képből JPEG készül EXIF-tájolással és legfeljebb 40 megapixeles konverziós bemenettel. HEIC/HEIF nem minden böngészőben dekódolható: érthető hiba, a hibás kép kivehető, a többi adat megmarad. Az imaging előnézet minden 1–8 EXIF-tájolást kezel.
- Legfeljebb 20 MiB bemeneti/kimeneti fotó, 200 MiB teljes mentés, multipart-ráhagyással. Hibánál nincs automatikus navigáció, az eredeti és konvertált fájl a nyitott lap memóriájában marad sikeres mentésig vagy a kép kézi kivételéig. Lapbezárás/újratöltés továbbra is adatvesztést okozhat; nincs még tartós offline fotó/outbox.

## Helyi hozzáférési modell és biztonsági kapu

A jelenlegi modell **csak a Windows-gépen megnyitott helyi alkalmazás**. Nem vezettünk be app-fiókot, sessiont vagy titkot. A `LocalAccessFilter` csak localhost/127.0.0.1/IPv6 loopback Hostot fogad, elutasítja a Forwarded/X-Forwarded fejléceket és a böngésző cross-site kéréseit. A szerver nem értelmezi át a proxyfejléceket (`server.forward-headers-strategy=none`). Ez a loopback kötés és a változatlan Docker portkötés mellett védelmi korlát, **nem önálló felhasználóhitelesítés**; a helyi gép folyamatai továbbra is megbízhatóak.

API-írásnál az alkalmazás `X-Otthontar-Request: 1` fejlécet küld; a szerver ezt kéri, és jelenlévő Origin esetén pontos eredetegyezést ellenőriz. Nincs CORS-engedélyezés, így idegen weboldal nem adhatja hozzá engedély nélkül ezt a fejlécet. A kézi API-író eszközöknek is küldeniük kell; a windowsos acceptance teszt közvetlen író kérése frissítve. A privát API/fotó és leltároldal HTTP-cache tiltott. Nincs session-cookie, ezért session-cookie attribútum most nem alkalmazható.

**Tailscale/reverse proxy forgalom jelenleg 403-at kap.** Ne kerüld meg a szűrőt Host-átírással vagy a proxyfejlécek eltüntetésével. A távoli modell elfogadása után külön feladatként kell szabályosan bővíteni: engedélyezett HTTPS host, jóváhagyott háztartási Tailscale-eszközök/ACL és szükség szerint szerveroldali app-login, CSRF, Secure/HttpOnly/SameSite cookie, kijelentkezés és mobilos újracsatlakozás tesztje. A hálózati védelem nem helyettesít automatikusan app-hitelesítést.

## Kézzel követendő HTTPS-terv – csak a biztonsági kapu lezárása után

1. Windowsra és iPhone-ra telepíts Tailscale-t, jelentkezz be a kiválasztott háztartási tailnetbe. Az adminfelületen csak a jóváhagyott felhasználók/eszközök kapjanak hozzáférést a webszolgáltatáshoz; ellenőrizd az ACL/grant szabályt.
2. Engedélyezd a MagicDNS-t és a tailnet HTTPS-tanúsítványait. A kiosztott gépnév a tanúsítvány-átláthatósági naplóban megjelenhet; ne használj érzékeny gépnevet.
3. Helyben ellenőrizd a `http://127.0.0.1:8740/leltar` címet. A MySQL, az imaging és a képmappa maradjon helyi/belső. Ne módosíts routert, ne nyiss nyilvános portot.
4. A tényleges Windows-telepítésen futtasd: `tailscale version`, `tailscale serve --help`, `tailscale serve status`. A telepített súgó az irányadó; szükség szerint rendszergazdai konzolt használj. **Ebben a munkában Tailscale nincs telepítve, a géped súgóját nem ellenőriztük.**
5. Csak a hozzáférési modell implementálása és jóváhagyása után: a súgóval ellenőrzött `tailscale serve 8740` vagy `tailscale serve http://127.0.0.1:8740` a localhostos webszolgáltatást szolgálhatja ki tailnetes HTTPS-sel. Először foreground módot használj. **Serve, nem Funnel.** A Funnel állapotát is ellenőrizd, ne legyen nyilvános közzététel. A jelenlegi helyi szűrővel ez a próba szándékosan nem működik.
6. iPhone-on legyen aktív a Tailscale VPN. Safariban nyisd meg a kijelzett `https://<host>.ts.net/leltar` címet. Tanúsítványhiba esetén állj meg; ne kapcsold ki a TLS-ellenőrzést. Mobilinternetről és Wi-Fi-ről is próbáld ki a hozzáférést.
7. Mindig ezt az egy HTTPS origint használd. A localhost, LAN-IP és másik host külön böngészőtárhely/telepítés; azok közt nincs automatikus adatátvitel.
8. Safari → Megosztás → Hozzáadás a Főképernyőhöz → Hozzáadás. Indítsd az ikonról. Mac és App Store nem szükséges. A telepítés nem automatikus.
9. Leállítás foreground módban Ctrl+C; a `tailscale serve status` alapján ellenőrizd az eredményt. Meglévő más szolgáltatás konfigurációját ne töröld tömeges `reset` paranccsal.

## Ellenőrzések és átadás

- `mvn verify`: egységtesztek, fordítás, csomagolás és Spotless. A buildhez teljes JDK 21+, Maven és Maven Central-elérés kell.
- `node tests/pwa.cjs`: Playwright Core + Chromium/Edge szükséges. `PLAYWRIGHT_CORE` és `BROWSER_EXE` megadható, ahogy a windowsos tesztnél. A teszt izolált, ideiglenes HTTP fixture-t indít véletlen localhost porton, **nem indít MySQL-t és nem teszteli a teljes Spring/Windows/Docker telepítést**.
- Böngészős lefedettség: mobil/desktop elrendezés, manifest/ikonok, online mentési redirect, JPEG-bájtmegőrzés, PNG→JPEG, nem támogatott HEIC, hibás mentés utáni piszkozat, méretkorlátok, offline mélylink, API-cache kizárás, megszakadt frissítés és késleltetett aktiválás, IndexedDB és idegen cache megőrzése.
- Java: helyi olvasás, no-store és keretezés elleni fejlécek, idegen Host/proxy/cross-site elutasítás, CSRF fejléc/Origin, EXIF mind a nyolc állása és mindkét bájtsorrendje, hibás EXIF offset, valódi JPEG-előnézet forgatása, 20 MiB határ, 413/422 hibák.
- A meglévő `tests/acceptance.cjs` Windows/MySQL/Edge végponttól végpontig próbáját az eredeti gépen kell futtatni a JPA-ág összefésülése után. A személyes adatokhoz nem nyúltunk.

### Valódi iPhone kézi tesztlista (a felhasználó végzi)

- [ ] A biztonsági kapu implementálva/tesztelve; nem engedélyezett eszköz nem éri el a leltárt vagy a fotókat.
- [ ] Egy megbízható HTTPS origin Safari alatt, telepítés főképernyőre, helyes név/ikon, önálló ablak.
- [ ] Álló/fekvő mód, 200% nagyítás, VoiceOver címkék, űrlapfókusz, billentyűzet, lista/adatlap/Beállítások vízszintes túlfutás nélkül.
- [ ] Kamera engedélyezés/elutasítás, fotótárból JPEG és HEIC, álló/fekvő/mirrored fotó és előnézet/original helyes tájolása.
- [ ] 20 MiB alatti, pontos határon lévő, nagyobb és 200 MiB-t meghaladó mentés; hiba után név/leírás/fotó megmarad a nyitott lapon.
- [ ] Sikeres online mentés, újranyitás, borítókép, eladás, költöztetés és lomtár meglévő működése.
- [ ] Mentés közben kapcsolatvesztés, visszakapcsolás és kézi újrapróbálás; nincs automatikus adatvesztő újratöltés.
- [ ] Első online megnyitás és Service Worker település után app bezárás, repülő mód/VPN kikapcsolása, újranyitás: offline tájékoztató. Ebben a fázisban a leltár még nem olvasható offline.
- [ ] Nyitott piszkozat mellett új alkalmazásverzió: csak értesítés; mentés és minden lap bezárása után frissülés.

## Következő fázis

A 02-es feladat előtt a JPA-átállás eredményét össze kell fésülni, az új helyi API-fejlécet meg kell őrizni, és a biztonságos telefonos elérés kapuját le kell zárni. Ekkor épülhet a teljes offline snapshot/előnézet és változásnapló. A 03-as fázis hozza a tartós offline szerkesztést, outboxot, konfliktuskezelést és visszaállítható exportot.

Források: [Service Worker](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API/Using_Service_Workers), [atomikus addAll](https://developer.mozilla.org/en-US/docs/Web/API/Cache/addAll), [EXIF-orientáció](https://developer.mozilla.org/en-US/docs/Web/API/Window/createImageBitmap), [Tailscale Serve](https://tailscale.com/docs/features/tailscale-serve), [Serve CLI](https://tailscale.com/docs/reference/tailscale-cli/serve).

### Futtatási eredmény – 2026. november 17.

- Maven 3.9.11 + teljes JDK 21: `spotless:apply verify` **BUILD SUCCESS**, összesen **10 Java-teszt**, 0 hiba, 0 kihagyott teszt. A futtatókörnyezetből hiányzó fejlesztőkészletet ideiglenesen pótoltuk; a repó verzióit nem módosítottuk.
- `tests/pwa.cjs`: mindhárom tesztcsoport sikeres, Chromium Headless Shell 131, mobilos 390×844 és asztali 1280×900 nézet. A rendszer Chromiumát környezeti indítási korlátozás miatt nem tudtuk használni, ezért külön headless böngészővel futott a próba.
- JavaScript szintaxis és `git diff --check`: sikeres.
- Valódi iPhone, Tailscale, Windowsos teljes acceptance és Docker/MySQL integráció ebben a körben **nem futott**.
