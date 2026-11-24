# Közös technikai tudástár – BorosCode / Otthontár

Utolsó frissítés: 2026-11-24
Forrás: felhasználói döntések és a GitHub-repók ellenőrzött dokumentációja.
Láthatóság: **NYILVÁNOS**. Ne kerüljön ide jelszó, token, személyes adat, ügyféladat, lakás- vagy pénzügyi adat, privát cím, teljes szerverkonfiguráció vagy más titok.

## Munkamenet-kezdési protokoll AI-asszisztenseknek

1. Az érintett feladat előtt olvasd el ezt a fájlt a `borosdavid/borosCode` repo `main` ágának **legfrissebb** verziójából.
2. Olvasd el az érintett repo `AGENTS.md`, `README.md` és szükséges technikai dokumentációját; a `borosCode` Otthontár-fejlesztésnél a `prompts/` és `roadmap/` hiteles forrása a `main`.
3. Vizsgáld meg a tényleges kódot, aktív ágat és telepítési állapotot. Ez a fájl összefoglaló, **nem bizonyítja** az aktuális működést.
4. A munka végén dokumentáld az új, ellenőrzött műszaki döntéseket és állapotokat, forrással vagy commit-hivatkozással, és írd be az ide vonatkozó változásokat; ellenőrizetlen állítást jelölj feltételezésnek. Ütközésnél ne írj felül más módosítását.
5. Ne írd ide a titkokat; használj szolgáltatói Secret/Variables kezelést vagy megfelelően védett lokális konfigurációt.
6. Másik gépen keletkezett, GitHubra **nem feltöltött** állapot nem látható innen. ChatGPT-beszélgetésben a GitHub-összeköttetés és a dokumentum aktív beolvasása szükséges; pusztán az azonos fiókba való bejelentkezés nem garantál automatikus fájlolvasást.

## Repók és felelősségi körök (ellenőrizve: 2026-11-24)

- `borosdavid/borosCode` – Otthontár alkalmazás, **nyilvános**; főág `main`. A repo `AGENTS.md` szabályozza a `dev`, `teszt`, `main` munkarendet. A `main` ág `prompts/` és `roadmap/` dokumentációja a hiteles tervezési forrás.
- `borosdavid/borosCode-website` – a `boroscode.com` nyilvános főoldalának külön repója, **nyilvános**; a repo README szerint a HTML: `public/index.html`, a CSS: `public/styles.css`, a Cloudflare Workers deploy: `npm run deploy`, a Worker konfigurációja: `wrangler.jsonc`.
- A két repót ne keverd össze. Az Otthontár alkalmazáskódját ne helyezd a weboldal-repóba, és a főoldal telepítési módosításait ne alkalmazd automatikusan az Otthontár kódjára.

## Cloudflare / weboldal – ismert helyzet

- A felhasználó megerősítette, hogy megvette a `boroscode.com` domaint a Cloudflare-nél.
- A `borosCode-website` README szerint a főoldal Cloudflare Workersön működő publikus weboldal, a `boroscode.com/otthontar/` útvonal pedig a külön Otthontár eredetkiszolgálóra irányít. A README leírja a Cloudflare Tunnel / Access service token / Worker Secrets tervezett vagy alkalmazott konfigurációját; az aktuális Cloudflare-fiókbeállítások külön ellenőrzendők.
- 2026-11-24-én a felhasználó képernyőképe szerint a `https://boroscode.com/otthontar/leltar` HTML válasz 200, de több, hash-elt nevű CSS és JavaScript asset 404-es hibával tér vissza. A felület formázatlan. **Hiba nyitott, ok nem igazolt**. Ellenőrizendő: HTML asset-hivatkozások, `/otthontar/` kontextusútvonal, proxy/Worker routing, build és origin statikus állományai, cache. A hiba miatt nem indokolt automatikusan újraírni a dizájnt.
- A Cloudflare-fiókhoz való közvetlen, hitelesített ChatGPT hozzáférés **nem igazolt**. Ne állítsd, hogy működő Cloudflare-kapcsolat van csak azért, mert GitHub hozzáférés van.
- Statikus főoldal működtetéséhez nem szükséges bejelentkezés vagy adatbázis. Cloudflare Secrets soha nem kerülhetnek Gitbe.

## Otthontár – műszaki összefoglaló

- Cél: háztartási leltár és költözéskezelő; később mobil offline használat és további modulok.
- A repo README szerint Java/Maven projekt; Inventory és külön Imaging komponens, MySQL; szerveroldali Thymeleaf megjelenítés; Docker Compose és helyi natív tesztfolyamat. Az aktuális implementációt a megfelelő ágban ellenőrizd.
- Tervként felmerült az iOS/offline kliens, később Android, naptár és további funkciók; ezek **nem tekinthetők elkészültnek** a kód és a státuszdokumentum ellenőrzése nélkül.
- A felhasználó Linux/Ubuntu otthoni szervert is használ, távoli hozzáféréshez korábban Tailscale, Cloudflare és iSH elemeket említett. A pontos élő útvonalak/szolgáltatások ellenőrizendők.
- Műszaki változtatás előtt őrizd meg a meglévő adatbázist, képeket, köteteket és lokális profilokat; ne futtass destruktív lépést ellenőrzés nélkül.
- A `borosCode/AGENTS.md` tartalmazza az ágszabályokat és a dokumentáció elsőbbségét; ezeket ne írd felül ezzel az összefoglalóval.

## Döntési napló

| Dátum | Döntés / igény | Állapot |
| --- | --- | --- |
| 2026-11-17 | Otthontár: `dev` fejlesztés, `teszt` átadás, `main` közös prompt/roadmap forrás; részletek az AGENTS.md-ben | Dokumentált munkarend |
| 2026-11 | A `boroscode.com` domain Cloudflare-en regisztrálva, a nyilvános főoldal külön GitHub-repóban kezelendő | Domainvásárlás felhasználó által megerősítve |
| 2026-11-24 | Otthontár éles elérésén több CSS/JS asset 404 | Nyitott hiba |
| 2026-11-24 | Egységes, gépek és AI-asszisztensek között olvasható technikai tudástár GitHubon | Ez a dokumentum |

## Változáskezelés

- Új tényhez rögzíts dátumot, forrást (repo, commit, vizsgálat vagy felhasználói döntés), megbízhatóságot és nyitott teendőt.
- A dokumentációt a tényleges állapottal tartsd összhangban, de ne moss össze terveket és megvalósult funkciókat.
- Ha az információ személyes vagy bizalmas, külön **privát**, megfelelően jogosultságkezelt tudástárba kerüljön; oda se másolj be hitelesítési titkokat.
