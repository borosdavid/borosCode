# BorosCode – domain és Cloudflare-elérés

> **Státusz:** Fejlesztés alatt
> **Pontosítás / akadály:** Az Ubuntu-példány Cloudflare Tunnel és Access mögött elérhető. A saját fiókos be- és kijelentkezés, a fotófeltöltés és az iPhone-próba hátravan.
> **Megvalósítás:** [Otthontár dev, c19ec97](https://github.com/borosdavid/borosCode/commit/c19ec9705ec67df3a4f7a6cf722d37dd6204abc9), [BorosCode weboldal main, 1c3eaea](https://github.com/borosdavid/borosCode-website/commit/1c3eaea1c733f3114b9035cae46eb921cf60fd1f).
> **Ellenőrzés:** 2026-11-23-án a Maven verify 20 Java-tesztje sikeres. A nyilvános HTTPS kezdőcím átirányít a belépőoldalra (302 → 200), a CSRF-végpont Secure és HttpOnly munkamenetsütit küld az aloldalhoz, a leltár API fiók nélkül 401-et ad, a háttérhost szolgáltatási token nélkül 403-at ad. Korábban a helyi MySQL-belépés, fotómentés, újraindítás, három Worker-teszt és a Wrangler próbatelepítése is sikeres volt.
> **Felhasználói teszt:** Nincs átadva.
> **Frissítve:** 2026-11-23. [Státuszszabályok](../projekt/STATUSZOK.md).

## A címek szerepe

| Cím | Szerep |
|---|---|
| `boroscode.com` | Nyilvános projektlista a BorosCode Workeren. |
| `boroscode.com/otthontar/` | Az Otthontár felhasználói címe az alkalmazás saját jelszavas belépésével. |
| `otthontar-origin.boroscode.com` | A Worker szolgáltatási tokenjével elérhető háttérhost. |
| `<projekt>.boroscode.com` | A további önálló fejlesztések opcionális címe. |

A [D-013 döntés](../projekt/DONTESI-NAPLO.md#d-013--otthontár-a-boroscode-főoldal-alatt) az eredeti D-012 közvetlen aldomaines tervét az Otthontár esetében felváltja. A jelenlegi háttérpéldány Ubuntu. A későbbi szerverköltözéskor a felhasználói URL megtartható. [Dedikált Debian otthoni szerver](DEDIKALT-DEBIAN-OTTHONI-SZERVER.md)

## Üzembe helyezés és próba

Az Ubuntu-példányhoz a loopback webszolgáltatás `http://127.0.0.1:18740`. A Tunnel a `boroscode.com` Host fejléc mellett `otthontar-origin.boroscode.com` értéket továbbít az `X-Forwarded-Host` fejlécben. A dev példányon ezt a `PROXY_FORWARDED_HOST` értékkel kell engedélyezni.

1. Az Ubuntu-példány fejlesztői frissítésének menete a [kódhoz tartozó útmutatóban](https://github.com/borosdavid/borosCode/blob/ffeecbe0e4745b99be76e194a85e80190971f3e2/docs/KNOWHOW-ubuntu-frissites.md) található. A tunnel az alkalmazás webszolgáltatásához továbbít. Az Access Service Auth szabálya és a tunnel JWT-ellenőrzése védi a háttérhostot. A főoldali Worker a két hozzá tartozó titkot a Cloudflare saját tárolójából olvassa. [Lépésről lépésre](https://github.com/borosdavid/borosCode-website/blob/71a04e009cfe5a159e595c708c3091c059409106/README.md#otthont%C3%A1r-a-f%C5%91oldalr%C3%B3l)
2. Asztali böngészőből és iPhone-ról ellenőrizzük a HTTPS-t, a be- és kijelentkezést, a leltárt és a fotófeltöltést. A háttérhost közvetlen hozzáférését is próbáljuk ki érvényes szolgáltatási token nélkül.
3. A korábbi helyi és Tailscale-cím külön böngészős origin. A mobileszközre telepített kezdőképernyős ikont és a meglévő offline adatokat az átállás előtt külön ellenőrizzük. A telefonos szerveradminisztráció iSH-ból, SSH-val történik a Tailscale hálózatán. [Hozzáférési útvonalak](../projekt/SZERVER-TAVOLI-HOZZAFERES.md).
