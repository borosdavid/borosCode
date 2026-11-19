# BorosCode – domain és Cloudflare-elérés

> **Státusz:** Tervezett
> **Pontosítás / akadály:** Dávid megvásárolta a `boroscode.com` domaint a Cloudflare-nél. A főoldal tárhelyét, az Otthontár hozzáférési szabályait és az átállás időpontját később választjuk ki.
> **Megvalósítás:** Az elfogadott irányt a [D-012 döntés](../projekt/DONTESI-NAPLO.md#d-012--boroscode-domain-és-cloudflare-elérés) rögzíti. A tervhez még nincs igazolt telepítés.
> **Ellenőrzés:** A közzététel előtti eszközpróbák hátravannak.
> **Felhasználói teszt:** Nincs átadva.
> **Frissítve:** 2026-11-19. [Státuszszabályok](../projekt/STATUSZOK.md).

## A címek szerepe

| Tervezett cím | Szerep |
|---|---|
| `boroscode.com` | Nyilvános, egyszerű projektlista és belépési pont. |
| `otthontar.boroscode.com` | Az Otthontár állandó HTTPS-címe, kizárólag engedélyezett felhasználóknak. |
| `<projekt>.boroscode.com` | Külön cím a további önálló fejlesztésekhez, a projekthez illő hozzáféréssel és tárhellyel. |

A főoldal tárhelye külön választható az Otthontár otthoni szerverétől. Az Otthontár állandó címe megőrizhető, amikor a kiszolgáló a Windows-gépről a Debian-szerverre kerül. [Dedikált Debian otthoni szerver](DEDIKALT-DEBIAN-OTTHONI-SZERVER.md)

## Hozzáférés és átállás

1. A jelenlegi iPhone-próba az elkülönített Windows-tesztpéldányon, a privát Tailscale-címen folytatódik. [01B tesztterv](../prompts/01B-WINDOWS-TAVOLI-ELERES-VEZERLES.md)
2. A saját domainre váltáskor a **Cloudflare Tunnel** az Otthontár webes végpontjához továbbítja a forgalmat. A böngésző felől **Cloudflare Access** korlátozza a hozzáférést a kijelölt felhasználókra. A tunnelnél engedélyezzük az Access-token ellenőrzését, és megtartjuk az Otthontár alkalmazásszintű bejelentkezését. A szerveradminisztráció privát Tailscale/SSH csatornán marad. [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/routing-to-tunnel/) · [Cloudflare Access](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/self-hosted-public-app/)
3. Az alkalmazás HTTPS- és proxybeállítását az `otthontar.boroscode.com` címhez igazítjuk. A régi Tailscale-cím és az új cím külön böngészős tárhelyet használ, ezért az offline adatokat és a főképernyős ikont az átállás előtt külön ellenőrizzük. [Böngészős tárhely és origin](https://developer.mozilla.org/en-US/docs/Web/Security/Defenses/Same-origin_policy)
4. Az átállás előtt ellenőrizzük a HTTPS-t iPhone-on és asztali gépen, a jogosulatlan hozzáférés elutasítását, a Safari-belépést, valamint a leltár és a képek betöltését. Dokumentáljuk, hogyan lehet visszatérni a privát Tailscale-elérésre.
