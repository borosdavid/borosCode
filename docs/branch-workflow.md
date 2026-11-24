# Fejlesztési és felhasználói tesztágak

| Ág | Szerep | Mikor frissül? |
|---|---|---|
| dev | Folyamatban lévő és elkészült fejlesztések közös ága | Minden elvégzett fejlesztés automatikus commitjával és pushával |
| teszt | A felhasználó által kipróbálható alkalmazáskód | Kód: csak kész scope és átadási ellenőrzések után; műszaki és tesztdokumentáció: a kóddal együtt |
| main | Az alkalmazás kódrepo főága | Alkalmazáskód külön kiadáskor; projektállapot és kiadási döntések, valamint hivatkozók a privát promptokra/roadmapekre |
| borosCode-knowledge/main | **Privát központi tervezési ág** | Minden prompt-, roadmap- és közös tudástár-változáskor |

A szabályokat a gyökér `AGENTS.md` is rögzíti a későbbi fejlesztési munkákhoz. A `dev` ág automatikus mentése a fejlesztő feladata, nem a munkakönyvtár minden fájlváltozását figyelő háttérfolyamat.

## Kezdőállapot – 2026. november 17.

- A `teszt` kezdetben a `main` `ce9fdd60810f403c2f2145e032c50600209faa25` kódját és ezt a munkarendet tartalmazza. Ez az ág induló alapállapota, nem új PWA-tesztkiadás.
- A `dev` ezen felül tartalmazza a 01-es PWA-fejlesztést (`54b86ec0dfa9886a4026aa2d43f6c83d8cdf89e3`).
- A PWA mobilos/asztali böngészőpróbái és 10 Java-tesztje sikeresek, de a telefonos HTTPS/jogosultsági hozzáférés még nincs beállítva. Ezért a teljes 01-es fázis egyelőre nem kerül a `teszt` ágra.
- Az asztali Work-környezetben végzett JPA-átállás eredményét még fel kell tölteni és össze kell fésülni. Az eredeti munkakörnyezetben előbb ellenőrizd a nem commitolt változásokat; ne válts ágat azok elvesztésével.

## Következő fejlesztések

1. Tiszta vagy biztonságosan megőrzött munkakönyvtárból frissítsd és nyisd meg a `dev` ágat.
2. Fejlessz, végezd el az adott változáshoz szükséges ellenőrzéseket, majd automatikusan commitolj és pusholj a `dev` ágra.
3. Ha a felhasználói próba minden előfeltétele megvan, adj át pontos tesztleírást, és a kiválasztott, ellenőrzött `dev` állapotot emeld a `teszt` ágra. Ne vigyél át vele későbbi, még félkész fejlesztést.
4. Az átemelés lehetőleg fast-forward legyen; eltérő ágaknál vizsgáld meg és fésüld össze az eltérést. Soha ne állítsd vissza kényszerrel a távoli ágat.

## Dokumentáció kezelése – 2026-11-24-i új szabály

A korábbi `borosCode/main/prompts/` és `roadmap/` hiteles forrását felváltotta a külön **privát** `borosdavid/borosCode-knowledge/main/projects/otthontar/` mappa. Ott történik a tervek, promptok, sablonok, indexek és `kesz/` archívumok szerkesztése és commitolása.

- Feladat előtt olvasd el a privát tudástár `AGENTS.md`, `projects/otthontar/README.md` és a konkrét prompt aktuális main változatát, továbbá az alkalmazás kódrepo `AGENTS.md` fájlját.
- Az Ubuntu/Windows Codex önálló privát GitHub-jogosultsága vagy friss helyi klón szükséges. A régi kódrepo útvonalain lévő hivatkozó fájlokból nem szabad promptot végrehajtani.
- A kódrepo `dev` és `teszt` ágán maradt történeti tervek nem hitelesek; ne másold vissza őket a `main` ágra. A `main` régi helyén hivatkozó fájlok vannak.
- A tényleges alkalmazáskódot továbbra is `dev` ágra pushold; a kódhoz kötött műszaki/tesztdokumentáció vele együtt halad. A részletes feladatstátuszok a kódrepo `main/projekt/` alatt élnek; a privát tudástár hivatkozik rájuk.
- A `dev` és `teszt` munkarendi `AGENTS.md` és `docs/branch-workflow.md` szabályait tartsd naprakészen, a munkarendi változás nem jelent félkész alkalmazáskód-összevonást.
- Ne használj force push-t és ne módosíts párhuzamos munkát ellenőrzés nélkül.

Ez fejlesztői munkarend, nem folyamatos háttérszinkron.

## Feladatok nyomon követése

A promptok és roadmapek egységes életciklusa: Tervezett → Fejlesztés alatt → Tesztelhető → Elfogadott. Az adatlapot és a bizonyítékokat a main ág [projekt/STATUSZOK.md](https://github.com/borosdavid/borosCode/blob/main/projekt/STATUSZOK.md) szabályai szerint, feladatkezdéskor és állapotváltozáskor frissítsd. A nem igazolt asztali munka nem kész megvalósítás; a puszta dokumentációfrissítés nem tesztkiadás.

A központi [projektállapotot](https://github.com/borosdavid/borosCode/blob/main/projekt/ALLAPOT.md) a main ágon frissítsd az adatlapokkal együtt: mi működik, mi blokkolt, mi következik, mely commitból származik az információ. A helyi vagy más beszélgetésben végzett munkát csak igazolás után vedd át.

Minden valóban átadott alkalmazás-tesztverzióhoz készíts kitöltött átadási lapot és változáslistát a kód mellett, majd annotált, nem átírható `teszt-ÉÉÉÉ.HH.NN.SORSZÁM` címkét a teszt ág pontos kiadási commitjára. Kövesd a main [tesztverzió-eljárását](https://github.com/borosdavid/borosCode/blob/main/projekt/TESZTVERZIOK.md). Dokumentációs változást vagy hiányzó hozzáférésű PWA-t ne címkézz átadott alkalmazásverziónak.

A fontos termék-, architektúra- és munkarendi döntéseket a main [döntési naplójában](https://github.com/borosdavid/borosCode/blob/main/projekt/DONTESI-NAPLO.md) rögzítsd indokkal, alternatívákkal és bizonyítékkal. Válaszd külön az elfogadott irányt és a még nyitott javaslatot; a régi döntés felváltásának előzménye maradjon meg. A `projekt/` központi tervei és sablonjai a promptokhoz hasonlóan csak mainben szerkesztendők.
