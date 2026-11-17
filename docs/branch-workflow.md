# Fejlesztési és felhasználói tesztágak

| Ág | Szerep | Mikor frissül? |
|---|---|---|
| dev | Folyamatban lévő és elkészült fejlesztések közös ága | Minden elvégzett fejlesztés automatikus commitjával és pushával |
| teszt | A felhasználó által kipróbálható alkalmazáskód | Kód: csak kész scope és átadási ellenőrzések után; műszaki és tesztdokumentáció: a kóddal együtt |
| main | A jelenlegi főág | Alkalmazáskód külön kiadáskor; promptok és roadmapek minden tervezési módosításkor |

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

## Dokumentáció kezelése

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


## Feladatok nyomon követése

A promptok és roadmapek egységes életciklusa: Tervezett → Fejlesztés alatt → Tesztelhető → Elfogadott. Az adatlapot és a bizonyítékokat a main ág [projekt/STATUSZOK.md](https://github.com/borosdavid/borosCode/blob/main/projekt/STATUSZOK.md) szabályai szerint, feladatkezdéskor és állapotváltozáskor frissítsd. A nem igazolt asztali munka nem kész megvalósítás; a puszta dokumentációfrissítés nem tesztkiadás.

A központi [projektállapotot](https://github.com/borosdavid/borosCode/blob/main/projekt/ALLAPOT.md) a main ágon frissítsd az adatlapokkal együtt: mi működik, mi blokkolt, mi következik, mely commitból származik az információ. A helyi vagy más beszélgetésben végzett munkát csak igazolás után vedd át.
