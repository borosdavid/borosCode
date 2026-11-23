# BorosCode – domain és Cloudflare-elérés

> **Státusz:** Fejlesztés alatt
> **Pontosítás / akadály:** A Windows-tesztpéldány Cloudflare Tunnel-, Access- és Worker-beállítása, majd a külső eszközös próba hátravan.
> **Megvalósítás:** [Otthontár dev, efb2d4e](https://github.com/borosdavid/borosCode/commit/efb2d4e489a6e7fabd847747ba2b6406c38fbec7), [BorosCode weboldal main, 71a04e0](https://github.com/borosdavid/borosCode-website/commit/71a04e009cfe5a159e595c708c3091c059409106).
> **Ellenőrzés:** 2026-11-23-án a Maven verify 19 tesztje, a helyi MySQL-belépés, fotómentés és újraindítási próba gyökér- és aloldalas módban, három Worker-teszt és a Wrangler próbatelepítése sikeres. Valódi Windows/iPhone/Cloudflare belépés még nem igazolt.
> **Felhasználói teszt:** Nincs átadva.
> **Frissítve:** 2026-11-23. [Státuszszabályok](../projekt/STATUSZOK.md).

## A címek szerepe

| Cím | Szerep |
|---|---|
| `boroscode.com` | Nyilvános projektlista a BorosCode Workeren. |
| `boroscode.com/otthontar/` | Az Otthontár felhasználói címe az alkalmazás saját jelszavas belépésével. |
| `otthontar-origin.boroscode.com` | A Worker szolgáltatási tokenjével elérhető háttérhost. |
| `<projekt>.boroscode.com` | A további önálló fejlesztések opcionális címe. |

A [D-013 döntés](../projekt/DONTESI-NAPLO.md#d-013--otthontár-a-boroscode-főoldal-alatt) az eredeti D-012 közvetlen aldomaines tervét az Otthontár esetében felváltja. Az ideiglenes szerver Windows, a későbbi szerverköltözéskor a felhasználói URL megtartható. [Dedikált Debian otthoni szerver](DEDIKALT-DEBIAN-OTTHONI-SZERVER.md)

## Üzembe helyezés és próba

1. Az elkülönített Windows-tesztpéldányt a friss dev kódból indítsuk. A tunnel csak a webszolgáltatáshoz továbbít. Az Access Service Auth szabálya és a tunnel JWT-ellenőrzése védi a háttérhostot. A főoldali Worker a két hozzá tartozó titkot a Cloudflare saját tárolójából olvassa. [Lépésről lépésre](https://github.com/borosdavid/borosCode-website/blob/71a04e009cfe5a159e595c708c3091c059409106/README.md#otthont%C3%A1r-a-f%C5%91oldalr%C3%B3l)
2. Windowsról és iPhone-ról ellenőrizzük a HTTPS-t, a be- és kijelentkezést, a leltárt és a fotófeltöltést. A háttérhost közvetlen hozzáférését is próbáljuk ki érvényes szolgáltatási token nélkül.
3. A korábbi helyi és Tailscale-cím külön böngészős origin. A mobileszközre telepített kezdőképernyős ikont és a meglévő offline adatokat az átállás előtt külön ellenőrizzük. A privát Tailscale-próba és a szerveradminisztráció terve a [01B feladatban](../prompts/01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md) követhető.
