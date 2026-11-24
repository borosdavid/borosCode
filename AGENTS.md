# Otthontár – fejlesztési munkarend

## Kötelező ágak és mentés

A felhasználó 2026. november 17-i utasítása:
„Készíts dev és teszt brancheket is, devbe mindig commitáld automatikusan a fejlesztéseket, tesztbe pedig csak akkor, ha már készen áll az általam történő tesztelésre.”

- Az alapértelmezett fejlesztési célág a `dev`. Minden elvégzett fejlesztést automatikusan commitolj és pusholj a `dev` ágra. Ehhez ne kérj újabb megerősítést.
- Dolgozhatsz elkülönített munkafán vagy feladatágon, de az eredmény a `dev` ágra is kerüljön. Egy csak helyi commit vagy csak feature ág nem teljesíti a mentési kérést.
- Az önálló, értelmes munkarészeket mentsd; ne várj a teljes roadmap elkészültére. Ha van ismert hiba vagy blokkolt ellenőrzés, dokumentáld. Titok, személyes konfiguráció, adatbázis, fotó, buildtermék nem kerülhet commitba.
- A `teszt` ág alkalmazáskódja kizárólag a felhasználónak átadható tesztverziókat tartalmazza. Nem a `dev` minden commitjának automatikus másolata.
- A `teszt` ágra akkor emelj át, ha a kijelölt feladat/scope kész, az ideillő automatizált ellenőrzések sikeresek, az adott felhasználói teszthez szükséges előfeltételek rendelkezésre állnak, és leírtad az indítást, a tesztlépéseket, az elvárt eredményeket és az ismert korlátokat.
- A valódi eszközön végzendő felhasználói próba természetesen maradhat hátra: éppen erre szolgál a `teszt`. Hiányzó implementáció, hozzáférés vagy futtatási előfeltétel viszont nem nevezhető kész felhasználói tesztverziónak.
- A tesztkiadáshoz dokumentáld a forrás `dev` commitot és az ellenőrzések eredményét. A felhasználónak jelezd az ágat és a tesztelés módját.
- A `main` ágra nem kerül automatikusan alkalmazásfejlesztés ettől a munkarendtől. A közös tervezési dokumentációra az alábbi kivétel érvényes.

## Promptok, roadmapek és verzióhoz kötött dokumentáció

**2026-11-24-i felhasználói döntés (a régi 2026-11-17-i szabály helyébe lép):** az Otthontár promptjai és roadmapjei, beleértve a `kesz/` archívumokat és indexeket, kizárólag a **privát** [`borosdavid/borosCode-knowledge/main/projects/otthontar/`](https://github.com/borosdavid/borosCode-knowledge/tree/main/projects/otthontar) repo alatt szerkesztendők. A kódrepo `prompts/` és `roadmap/` régi fájljai csak átirányítók; a `dev` és `teszt` ágon esetleg megmaradt régi másolatok nem hitelesek.

- Feladatkezdéskor olvasd el a privát tudástár `AGENTS.md`, `projects/otthontar/README.md`, `projects/otthontar/prompts/00-KOZOS-UTASITASOK.md` és a kiválasztott prompt **legfrissebb main** változatát, és jegyezd fel a tudástár commitját. Ehhez a helyi/Ubuntu Codexnek külön privát GitHub-hozzáférés vagy friss klón kell. Ha nincs, kérj hozzáférést; ne használj elavult másolatot.
- Új promptot, roadmapet és archiválást a privát tudástár `main` ágába commitolj. A részletes projektstátuszok és döntési bizonyítékok továbbra is a `borosCode/main/projekt/` mappában élnek, a közös keresztprojektes állapot pedig a privát tudástárban hivatkozik rájuk. A terv nincs automatikusan megvalósítva.
- Az alkalmazásfejlesztés továbbra is a `dev` ágra kerül; a `teszt` ágra csak az ellenőrzött átadási feltételek után emelj át. Ne merge-öld a teljes privát tudástárat az alkalmazáskódba.
- A műszaki dokumentáció, API, tesztjegyzőkönyvek és kiadási útmutatók a hozzájuk tartozó kódot követik: `dev`, majd ellenőrzött `teszt`, külön kiadáskor `main`.
- Az `AGENTS.md` és `docs/branch-workflow.md` munkarendi szabályait minden használt ágon tartsd naprakészen, csak a szabályváltozások átvitelével. Ne használj force push-t, és a párhuzamos változásokat fésüld össze.

Ez nem folyamatos háttérszinkron: a privát GitHub-változatot minden munkamenetnek ténylegesen be kell olvasnia.

## Együttműködés és adatvédelem

- Módosítás előtt frissítsd és ellenőrizd a távoli ágakat. Párhuzamos asztali Work-fejlesztés is lehet folyamatban; a nem pusholt változásokat nem látod.
- Ne használj force push-t és ne írd felül más munkáját. Távoli ág változásakor olvasd újra, majd szabályosan fésüld össze a saját változtatásaidat.
- A meglévő személyes `.local/` profilt, adatbázist, fényképeket és Docker-köteteket őrizd meg. Meglévő Flyway-migrációt ne módosíts.
- Olvasd el az aktuális feladatot és a közös promptutasításokat. A fenti, felhasználó által megadott ágmunkarend elsőbbséget élvez a régebbi, commit/push előtti megerősítésre vonatkozó szöveggel szemben.
- A beszámolóban különítsd el az elkészült, ellenőrzött és még felhasználói tesztre váró részeket.

## Elkészült feladatleírások automatikus rendezése

A felhasználó kérése alapján a teljesen elkészült folyamatok leírásait automatikusan helyezd át a megfelelő `kesz/` almappába:

- `borosCode-knowledge/projects/otthontar/prompts/<feladat>.md` → `.../prompts/kesz/<feladat>.md`
- `borosCode-knowledge/projects/otthontar/roadmap/<terv>.md` → `.../roadmap/kesz/<terv>.md`

Ezt a feladat lezárásának részeként végezd el külön kérés nélkül, a dokumentáció áthelyezését és indexfrissítését külön commitban a **privát tudástár main** ágára mentsd; a hozzá tartozó fejlesztést a **borosCode dev** ágra commitold/pushold. Ez fejlesztői munkarend, nem időzített háttérfolyamat.

Áthelyezés előtt ellenőrizd a tényleges kódot, az adott feladat elfogadási feltételeit és a szükséges teszteredményeket. A részben elkészült, blokkolt, még nem hozzáférhető vagy kötelező elfogadási próbára váró feladat leírása az aktív mappában maradjon; rögzítsd, mi hiányzik. A pusztán elkészített prompt nem kész megvalósítás. Roadmap-tervet csak a teljes leírt cél teljesülésekor archiválj; részfeladat befejezésekor frissítsd a státuszt, de a tágabb terv maradjon aktív.

Az áthelyezett leírást őrizd meg teljes egészében. Egészítsd ki lezárási dátummal, a megvalósító commit(ok)/PR hivatkozásával, a teszteredményekkel és az átadási dokumentum helyével. Ne találj ki bizonyítékot. Az aktív szülőindexből a befejezett feladatot vedd ki az aktív sorrendből, és hivatkozd az archívum indexében. Javítsd a befelé és kifelé mutató relatív linkeket és a régi útvonalra hivatkozó dokumentumokat/szkripteket. Azonos nevű meglévő archív fájlt ne írj felül.

A privát tudástár `README.md` indexei, a `projects/otthontar/prompts/00-KOZOS-UTASITASOK.md` és a `projects/otthontar/prompts/SABLON.md` az aktív mappában maradnak, mert folyamatosan használt útmutatók. Újranyitott feladatnál a leírás visszakerülhet az aktív mappába, a korábbi lezárási előzmény megőrzésével. Az archiválás nem helyettesíti a `teszt` ágra emelés külön feltételeit.

## Feladatok nyomon követése

A promptok és roadmapek egységes életciklusa: Tervezett → Fejlesztés alatt → Tesztelhető → Elfogadott. Az adatlapot és a bizonyítékokat a main ág [projekt/STATUSZOK.md](https://github.com/borosdavid/borosCode/blob/main/projekt/STATUSZOK.md) szabályai szerint, feladatkezdéskor és állapotváltozáskor frissítsd. A nem igazolt asztali munka nem kész megvalósítás; a puszta dokumentációfrissítés nem tesztkiadás.

A központi [projektállapotot](https://github.com/borosdavid/borosCode/blob/main/projekt/ALLAPOT.md) a main ágon frissítsd az adatlapokkal együtt: mi működik, mi blokkolt, mi következik, mely commitból származik az információ. A helyi vagy más beszélgetésben végzett munkát csak igazolás után vedd át.

Minden valóban átadott alkalmazás-tesztverzióhoz készíts kitöltött átadási lapot és változáslistát a kód mellett, majd annotált, nem átírható `teszt-ÉÉÉÉ.HH.NN.SORSZÁM` címkét a teszt ág pontos kiadási commitjára. Kövesd a main [tesztverzió-eljárását](https://github.com/borosdavid/borosCode/blob/main/projekt/TESZTVERZIOK.md). Dokumentációs változást vagy hiányzó hozzáférésű PWA-t ne címkézz átadott alkalmazásverziónak.

A fontos termék-, architektúra- és munkarendi döntéseket a kódrepo main [döntési naplójában](https://github.com/borosdavid/borosCode/blob/main/projekt/DONTESI-NAPLO.md) rögzítsd indokkal, alternatívákkal és bizonyítékkal. Válaszd külön az elfogadott irányt és a még nyitott javaslatot; a régi döntés felváltásának előzménye maradjon meg. A `projekt/` központi tervei és sablonjai a promptokhoz hasonlóan csak mainben szerkesztendők.

## Közös, több gépen használható tudástár

**Kötelező munkamenet-kezdés:** minden fejlesztési, telepítési vagy rendszerbeállítási feladat megkezdésekor olvasd el a különálló, **privát** [`borosdavid/borosCode-knowledge`](https://github.com/borosdavid/borosCode-knowledge) repó `main` ágának legfrissebb `AGENTS.md`, `knowledge/KOZOS_TUDASTAR.md`, `knowledge/ALLAPOT.md` fájljait, szükség esetén a `knowledge/DONTESI_NAPLO.md` fájlt is. **Ez az egyetlen központi, hiteles tudástár** mind a nyilvános, mind a nem nyilvános projektinformációkhoz. A helyi `knowledge/KOZOS_TUDASTAR.md` csak átirányító.

A feladatspecifikus promptokat, a projekt saját státuszait, a valódi kódot és a fenti ágszabályokat továbbra is ellenőrizd. Az ellenőrzött, közös jelentőségű új kontextust és döntést a privát tudástárba dokumentáld, külön commitban a privát repo `main` ágán; a `borosCode` alkalmazásfejlesztési ágszabályait ez nem módosítja. Ha a privát repo nem olvasható, jelezd, ne találgass. Jelszót, API tokent, privát kulcsot és érzékeny adatállományt privát GitHub-repóba se commitolj. A GitHubra nem commitolt helyi és más beszélgetésbeli munkát ez továbbra sem teszi automatikusan láthatóvá.
