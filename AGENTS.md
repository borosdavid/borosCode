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

A felhasználó 2026. november 17-i döntése alapján a `prompts/` és `roadmap/` hiteles, aktuális forrása a `main` ág, beleértve a `kesz/` archívumokat és indexeket. Ez felváltja a korábbi, mindhárom ágra kötelező dokumentációszinkront.

- Új terveket, promptokat, státuszokat és archiválásokat kizárólag a `main` ágon szerkessz és commitolj/pusholj, külön dokumentációs commitban. Ehhez nem szükséges kész alkalmazásverzió vagy újabb megerősítés.
- A `dev` és `teszt` ágakon meglévő példányok maradhatnak, és normál Git-összevonással frissülhetnek, de nem hiteles források. Nem kell őket törölni vagy automatikusan szinkronizálni.
- Feladatkezdéskor frissítsd a távoli `main` és a munkához használt ág állapotát. A promptot és közös utasításait az `origin/main` megfelelő commitjából olvasd (például `git show origin/main:prompts/<feladat>.md`), és rögzítsd a felhasznált `main` commitot a fejlesztési/átadási riportban. Ehhez ne merge-öld a teljes `main` ágat a munkádba.
- Fejlesztés közben felmerülő tervpontosítást külön, tiszta munkafán vagy dokumentációs commitban ments a `main` ágra; ne keverd bele az alkalmazáskódot, és ne veszítsd el a helyi fejlesztést.
- A tervek státuszai a teljes projektre vonatkoznak. Add meg a megvalósítás ágát és commitját, az ellenőrzések eredményét és a felhasználói teszt állapotát. A `main` tervei előrébb járhatnak az ottani alkalmazáskódnál.
- A műszaki dokumentáció (például telepítés, API, konfiguráció, architektúra), a teszteredmények és az átadási útmutatók a hozzájuk tartozó kódot követik: a `dev` ágon készülnek, a kóddal együtt kerülnek a `teszt`, majd külön kiadáskor a `main` ágra. A gyökér `README.md` futtatási állításai is az adott ág kódját írják le.
- Az `AGENTS.md` és a `docs/branch-workflow.md` közös munkarendi szabályait minden használt ágon tartsd aktuálisan, kizárólag a szabályváltozások átvitelével. E kivétel nem jelenti az összes dokumentáció szinkronizálását.
- A távoli ág változásakor ellenőrizd és fésüld össze az új módosításokat; ne használj force push-t vagy vak könyvtármásolást. A `main` tervmódosításai miatt ne emelj át félkész alkalmazáskódot.

Ez fejlesztői munkarend, nem folyamatos háttérszinkron.

## Együttműködés és adatvédelem

- Módosítás előtt frissítsd és ellenőrizd a távoli ágakat. Párhuzamos asztali Work-fejlesztés is lehet folyamatban; a nem pusholt változásokat nem látod.
- Ne használj force push-t és ne írd felül más munkáját. Távoli ág változásakor olvasd újra, majd szabályosan fésüld össze a saját változtatásaidat.
- A meglévő személyes `.local/` profilt, adatbázist, fényképeket és Docker-köteteket őrizd meg. Meglévő Flyway-migrációt ne módosíts.
- Olvasd el az aktuális feladatot és a közös promptutasításokat. A fenti, felhasználó által megadott ágmunkarend elsőbbséget élvez a régebbi, commit/push előtti megerősítésre vonatkozó szöveggel szemben.
- A beszámolóban különítsd el az elkészült, ellenőrzött és még felhasználói tesztre váró részeket.

## Elkészült feladatleírások automatikus rendezése

A felhasználó kérése alapján a teljesen elkészült folyamatok leírásait automatikusan helyezd át a megfelelő `kesz/` almappába:

- `prompts/<feladat>.md` → `prompts/kesz/<feladat>.md`
- `roadmap/<terv>.md` → `roadmap/kesz/<terv>.md`

Ezt a feladat lezárásának részeként végezd el külön kérés nélkül, a dokumentáció áthelyezését és indexfrissítését külön commitban a `main` ágra mentsd; a hozzá tartozó fejlesztést a `dev` ágra commitold/pushold. Ez fejlesztői munkarend, nem időzített háttérfolyamat.

Áthelyezés előtt ellenőrizd a tényleges kódot, az adott feladat elfogadási feltételeit és a szükséges teszteredményeket. A részben elkészült, blokkolt, még nem hozzáférhető vagy kötelező elfogadási próbára váró feladat leírása az aktív mappában maradjon; rögzítsd, mi hiányzik. A pusztán elkészített prompt nem kész megvalósítás. Roadmap-tervet csak a teljes leírt cél teljesülésekor archiválj; részfeladat befejezésekor frissítsd a státuszt, de a tágabb terv maradjon aktív.

Az áthelyezett leírást őrizd meg teljes egészében. Egészítsd ki lezárási dátummal, a megvalósító commit(ok)/PR hivatkozásával, a teszteredményekkel és az átadási dokumentum helyével. Ne találj ki bizonyítékot. Az aktív szülőindexből a befejezett feladatot vedd ki az aktív sorrendből, és hivatkozd az archívum indexében. Javítsd a befelé és kifelé mutató relatív linkeket és a régi útvonalra hivatkozó dokumentumokat/szkripteket. Azonos nevű meglévő archív fájlt ne írj felül.

A `README.md` indexek, a `prompts/00-KOZOS-UTASITASOK.md` és a `prompts/SABLON.md` az aktív mappában maradnak, mert folyamatosan használt útmutatók. Újranyitott feladatnál a leírás visszakerülhet az aktív mappába, a korábbi lezárási előzmény megőrzésével. Az archiválás nem helyettesíti a `teszt` ágra emelés külön feltételeit.
