# Fejlesztési és felhasználói tesztágak

| Ág | Szerep | Mikor frissül? |
|---|---|---|
| dev | Folyamatban lévő és elkészült fejlesztések közös ága | Minden elvégzett fejlesztés automatikus commitjával és pushával |
| teszt | A felhasználó által kipróbálható verzió | Csak kész scope, sikeres releváns ellenőrzések és átadási tesztleírás után |
| main | A jelenlegi főág | A mostani áglétrehozás nem változtatja meg |

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
