# 01 – iPhone-kompatibilis PWA és biztonságos elérés

> **Státusz:** Fejlesztés alatt  
> **Pontosítás / akadály:** A felhasználó szerint az Otthontár weboldala jelenleg megfelelően működik az Ubuntu/Cloudflare útvonalon. A PWA telepítési, kamera- és offline indítási próbája valódi iPhone-on még igazolásra vár. A következő fejlesztési feladat a [01C webes felület](01C-WEB-FELULET.md).  
> **Megvalósítás:** dev; [PWA-fejlesztés](https://github.com/borosdavid/borosCode/commit/54b86ec0dfa9886a4026aa2d43f6c83d8cdf89e3).  
> **Ellenőrzés:** A korábbi [PWA-riport](https://github.com/borosdavid/borosCode/blob/6824ba30bcaa634cc8d96bef134d32774c4f5e75/docs/pwa-01.md) 10 Java-tesztet és 3 sikeres böngészős csoportot közölt. A 2026-11-17-i Windowsos JPA-összeillesztés után újrafuttatva: 24 Java-teszt és 7 teljes alkalmazás-acceptance csoport sikeres; a külön PWA mock tesztben 2 csoport sikeres, az offline deep-link csoport sikertelen. A PWA-források nem változtak az összeillesztésben; [dev riport](https://github.com/borosdavid/borosCode/blob/dae1224627a983217296ffe2dd47a339caacdb59/docs/jpa-dev-integracio.md). A 2026-11-24-i `ffeecbe` [CI-futás](https://github.com/borosdavid/borosCode/actions/runs/45331467488) Java/MySQL- és PowerShell-ellenőrzése sikeres, az Edge és a Chromium a tárgylista megjelenésénél időtúllépéssel leállt.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-24. [Státuszszabályok](../projekt/STATUSZOK.md).


Olvasd el a `prompts/00-KOZOS-UTASITASOK.md` közös szabályait és a gyökér `README.md`-t. Dolgozz a tényleges kód alapján.

## Feladat
Hozd létre a meglévő Otthontár alkalmazás telepíthető, mobilbarát PWA-alapját úgy, hogy a Windows/asztali felület és meglévő backend működése ne sérüljön. Ez a fázis az alap infrastruktúra; még NE ígérj teljes offline leltárt vagy szinkronizációt.

## Követelmények
1. Auditáld a Thymeleaf layoutot, `inventory.js`/CSS-t, statikus erőforrásokat, oldalsó API-hívásokat, session/auth megoldást, Docker és natív indítást. Röviden dokumentáld a jelenlegi állapotot és az iPhone-os akadályokat.
2. PWA manifest: alkalmazásnév Otthontár, magyar nyelv, megfelelő ikonok (különböző méretben), `display: standalone`, megfelelő `start_url`, `scope`, iOS meta és mobil viewport. Ne készíts felesleges párhuzamos frontend keretrendszert, ha a meglévő megoldás bővíthető.
3. Service Worker: verziózott cache, csak megfelelő statikus shell fájlok, tiszta update-stratégia, offline indító/hiba képernyő, teljes leltár cache-elését még NE csináld. Ne cache-elj hitelesítést vagy érzékeny API-válaszokat véletlenül HTTP Cache-ben. Cache-verziófrissítés ne töröljön IndexedDB-adatokat.
4. Mobilos használhatóság: érintésbarát tárgylista, tárgyadatlap és fotókezelési alap (ha szükséges, elkülönített mobilnézet), hozzáférhetőség, állapotjelzések. A mentések működjenek a meglévő API-n, ha a szerver elérhető.
5. A kamera eléréséhez fogadj fotót iPhone-ról (`input type=file accept=image/* capture=environment` vagy biztonságos kontextusban kamera API); kezeld iOS HEIC/HEIF bemenet lehetséges kompatibilitási kérdéseit, JPEG szerveroldali elvárását és EXIF-orientációt, méretkorlátot. Az eredeti kép átalakítása előtt ne veszíts adatot; adj felhasználóbarát hibaüzenetet, ha a konverzió nem lehetséges.
6. Ellenőrizd a meglévő Cloudflare-es HTTPS-elérést a kanonikus alkalmazáscímen. A már működő tunnel és az alkalmazásfiókos belépés maradjon használható. A privát szerverkarbantartás a felhasználó iSH/Tailscale SSH-útvonalán történik.
7. Adjon Safari „Megosztás → Hozzáadás a Főképernyőhöz” telepítési útmutatót, és külön írd le, hogy az eszközön való ellenőrzést a felhasználó végzi.

## Elfogadási feltételek
- A meglévő webalkalmazás PC-n változatlanul működik.
- HTTPS originről megnyitva a webapp rendelkezik manifesttel, regisztrált Service Workerrel és kezdőképernyős PWA-ként telepíthető.
- iPhone Safari alatt a mobilos felület elrendezése és a képfelvétel működése manuálisan ellenőrizhető (a fejlesztő adjon konkrét listát).
- Internet nélkül legalább a PWA app-shell és tájékoztató képernyő betöltődik; még NEM elvárás a teljes leltár.
- Automata tesztek és `mvn verify` vagy az adott projektben releváns alternatívák futtatása; ha nem futtatható, ok dokumentálva.

## Pontosítás a hálózathoz és a telepítéshez
- Az iPhone-os átadási próba címe `https://boroscode.com/otthontar/`. Teszteld a Cloudflare útvonalon az alkalmazásbelépést, a fotót és a kezdőképernyős telepítést. A [01B Windows/Tailscale Serve kör](01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md) külön elfogadási úton halad.
- A `start_url`, `scope`, a Service Worker hatóköre és az API-, kép- és navigációs útvonalak kövessék az `/otthontar/` előtagot. A böngészőteszt fedje le a gyökérútvonalon futó helyi példányt és az alútvonalat is, beleértve a közvetlen adatlapmegnyitást és az offline indítóképernyőt.
- A Service Worker telepítési/cache stratégiája ne cache-eljen véletlenül `POST`-ot, hitelesítési választ vagy privát API-JSON-t. Az offline app-shell cache frissítését verziózottan, megszakadáskor visszaállíthatóan kezeld; a böngészős „hard refresh” nem törölhet fel nem töltött IndexedDB adatot.
- Kamera: `capture` csak **választható eszközpreferencia**, nem garantált kamera-UI. Támogasd a Fotók kiválasztását és a kamera engedélyezésének elutasítását is. Az iOS által adott HEIC/HEIF és EXIF irány kezelése legyen ellenőrzött; az `image/jpeg` végpontba csak tényleges JPEG kerüljön. Teszteld a JPEG-enkénti 20 MiB és mentésenkénti 200 MiB backendhatárt, valamint külön a Cloudflare útvonal alacsonyabb kérésméret-határát. A túl nagy vagy megszakadt kérés után maradjon meg a piszkozat.
- iPhone Safari alatt ellenőrizd az alkalmazásbelépést és a „Hozzáadás a Főképernyőhöz” lépést a Cloudflare-es címen. A valós eszközön végzett próba eredményét rögzítsd az átadásban.

## Biztonsági kapu (a 01-es fázis része, nem a 04-esre halasztható)
- Az aktuális `dev` megvalósítás alkalmazásszintű belépését a 01A írja le. Mielőtt a Cloudflare-es útvonalat átadásra késznek jelölöd, ellenőrizd a D-008 szerinti alkalmazásfiókot, CSRF-védelmet, session/cookie-szabályokat, a képek és az API jogosultságait, valamint a mobilos újracsatlakozást.
- Dokumentáld a Cloudflare mögötti alkalmazásbelépés és a jogosultság-ellenőrzések eredményét. Szerepeljen benne a védett végpontok és a szolgáltatási hitelesítés nélküli közvetlen háttérhost ellenőrzése.
- A kamera előkészítése az 01-ben **online feltöltést** jelent; még nem offline fotózást. Az offline mentés/outbox eredeti-fotó megőrzéssel a 03-as fázis feladata.
- Az API-útvonalak az alkalmazás telepítési előtagjához igazodjanak (`/api/...` helyben, `/otthontar/api/...` a fődomainen). A statikus shell cache kizárólag a kijelölt statikus erőforrásokat tartalmazza.

## Kötelező fiókpontosítás – D-008, 2026-11-17

A hozzáférési modell eldőlt: felhasználónév/jelszó szükséges; minden aktív bejelentkezett felhasználó minden alkalmazásfunkcióhoz és közös adathoz hozzáfér. Az API-k és képek is védettek. A Tailscale önmagában nem helyettesíti ezt. A részletes [01A feladat](01A-FELHASZNALOI-FIOKOK.md) az 01-es fázis telefonos átadásának előfeltétele. Az offline session/cache/fiókváltás szerződését a 02/03-as fázisban is tartsd meg. A tervezés önmagában nem kész hitelesítés.
