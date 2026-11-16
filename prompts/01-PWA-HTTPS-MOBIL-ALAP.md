# 01 – iPhone-kompatibilis PWA és biztonságos elérés

Olvasd el a `prompts/00-KOZOS-UTASITASOK.md` közös szabályait és a gyökér `README.md`-t. Dolgozz a tényleges kód alapján.

## Feladat
Hozd létre a meglévő Otthontár alkalmazás telepíthető, mobilbarát PWA-alapját úgy, hogy a Windows/asztali felület és meglévő backend működése ne sérüljön. Ez a fázis az alap infrastruktúra; még NE ígérj teljes offline leltárt vagy szinkronizációt.

## Követelmények
1. Auditáld a Thymeleaf layoutot, `inventory.js`/CSS-t, statikus erőforrásokat, oldalsó API-hívásokat, session/auth megoldást, Docker és natív indítást. Röviden dokumentáld a jelenlegi állapotot és az iPhone-os akadályokat.
2. PWA manifest: alkalmazásnév Otthontár, magyar nyelv, megfelelő ikonok (különböző méretben), `display: standalone`, megfelelő `start_url`, `scope`, iOS meta és mobil viewport. Ne készíts felesleges párhuzamos frontend keretrendszert, ha a meglévő megoldás bővíthető.
3. Service Worker: verziózott cache, csak megfelelő statikus shell fájlok, tiszta update-stratégia, offline indító/hiba képernyő, teljes leltár cache-elését még NE csináld. Ne cache-elj hitelesítést vagy érzékeny API-válaszokat véletlenül HTTP Cache-ben. Cache-verziófrissítés ne töröljön IndexedDB-adatokat.
4. Mobilos használhatóság: érintésbarát tárgylista, tárgyadatlap és fotókezelési alap (ha szükséges, elkülönített mobilnézet), hozzáférhetőség, állapotjelzések. A mentések működjenek a meglévő API-n, ha a szerver elérhető.
5. A kamera eléréséhez fogadj fotót iPhone-ról (`input type=file accept=image/* capture=environment` vagy biztonságos kontextusban kamera API); kezeld iOS HEIC/HEIF bemenet lehetséges kompatibilitási kérdéseit, JPEG szerveroldali elvárását és EXIF-orientációt, méretkorlátot. Az eredeti kép átalakítása előtt ne veszíts adatot; adj felhasználóbarát hibaüzenetet, ha a konverzió nem lehetséges.
6. Dokumentáld és szükség szerint készíts biztonságos HELYI/TÁVOLI hozzáférési tervet az otthoni Windows géphez (HTTPS az iPhone-on megbízható tanúsítvánnyal; pl. VPN/Tailscale és HTTPS reverse proxy vagy megfelelő HTTPS-elérés). **Ne nyiss nyilvános portot automatikusan, ne változtass routert, ne kapcsold ki a TLS-ellenőrzést.** A konkrét hálózati beállítások külön, jóváhagyandó kézi lépések legyenek. Ha nincs HTTPS, mutasd meg, miért nem teljesül a telepítés/kamera/Service Worker cél.
7. Adjon Safari „Megosztás → Hozzáadás a Főképernyőhöz” telepítési útmutatót, és külön írd le, hogy az eszközön való ellenőrzést a felhasználó végzi.

## Elfogadási feltételek
- A meglévő webalkalmazás PC-n változatlanul működik.
- HTTPS originről megnyitva a webapp rendelkezik manifesttel, regisztrált Service Workerrel és kezdőképernyős PWA-ként telepíthető.
- iPhone Safari alatt a mobilos felület elrendezése és a képfelvétel működése manuálisan ellenőrizhető (a fejlesztő adjon konkrét listát).
- Internet nélkül legalább a PWA app-shell és tájékoztató képernyő betöltődik; még NEM elvárás a teljes leltár.
- Automata tesztek és `mvn verify` vagy az adott projektben releváns alternatívák futtatása; ha nem futtatható, ok dokumentálva.

## Pontosítás a hálózathoz és a telepítéshez
- Elsőként ajánld a **Tailscale Serve (nem Funnel)** HTTPS reverse proxy-t a helyben maradó `127.0.0.1:8740` elé, ha a felhasználó ezt választja. A `tailscale serve` CLI-verziófüggő: a tényleges telepített Tailscale súgóját használd, és ne készíts automatikusan hálózatot megnyitó konfigurációt. Tailscale Windows telepítés, mobil VPN, MagicDNS, HTTPS tanúsítvány és iPhone Safari biztonságos origin legyen részletes, *kézzel követendő* ellenőrzőlista.
- A telepítési URL és valamennyi API/fotó URL legyen **same-origin, relatív elérési út**. Ellenőrizd a `/leltar` alatti belépőlapot, PWA `scope`, `start_url`, Service Worker scope és navigációk illeszkedését. PWA külön eredetként viselkedik LAN-IP-ről, localhostról és tailnetes HTTPS hostról; ne ígérd, hogy a helyi adatbázisuk azonos.
- A Service Worker telepítési/cache stratégiája ne cache-eljen véletlenül `POST`-ot, hitelesítési választ vagy privát API-JSON-t. Az offline app-shell cache frissítését verziózottan, megszakadáskor visszaállíthatóan kezeld; a böngészős „hard refresh” nem törölhet fel nem töltött IndexedDB adatot.
- Kamera: `capture` csak **választható eszközpreferencia**, nem garantált kamera-UI. Támogasd a Fotók kiválasztását és a kamera engedélyezésének elutasítását is. Az iOS által adott HEIC/HEIF és EXIF irány kezelése legyen ellenőrzött; az `image/jpeg` végpontba csak tényleges JPEG kerüljön. Teszteld a maximális 20 MiB/JPEG és 200 MiB/mentés szerverhatárt, és mobilos hiba esetén ne veszítsd el a piszkozatot.
- **Ne állítsd, hogy a PWA automatikusan települ vagy bárhonnan online elérhető**, amíg iPhone-on Tailscale VPN/HTTPS és Safari „Hozzáadás a Főképernyőhöz” próba nem történt meg. A PWA használatához Mac nem kell.
