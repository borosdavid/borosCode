# Otthontár – fejlesztési munkarend

## Kötelező ágak és mentés

A felhasználó 2026. november 17-i utasítása:
„Készíts dev és teszt brancheket is, devbe mindig commitáld automatikusan a fejlesztéseket, tesztbe pedig csak akkor, ha már készen áll az általam történő tesztelésre.”

- Az alapértelmezett fejlesztési célág a `dev`. Minden elvégzett fejlesztést automatikusan commitolj és pusholj a `dev` ágra. Ehhez ne kérj újabb megerősítést.
- Dolgozhatsz elkülönített munkafán vagy feladatágon, de az eredmény a `dev` ágra is kerüljön. Egy csak helyi commit vagy csak feature ág nem teljesíti a mentési kérést.
- Az önálló, értelmes munkarészeket mentsd; ne várj a teljes roadmap elkészültére. Ha van ismert hiba vagy blokkolt ellenőrzés, dokumentáld. Titok, személyes konfiguráció, adatbázis, fotó, buildtermék nem kerülhet commitba.
- A `teszt` ág kizárólag a felhasználónak átadható tesztverziókat tartalmazza. Nem a `dev` minden commitjának automatikus másolata.
- A `teszt` ágra akkor emelj át, ha a kijelölt feladat/scope kész, az ideillő automatizált ellenőrzések sikeresek, az adott felhasználói teszthez szükséges előfeltételek rendelkezésre állnak, és leírtad az indítást, a tesztlépéseket, az elvárt eredményeket és az ismert korlátokat.
- A valódi eszközön végzendő felhasználói próba természetesen maradhat hátra: éppen erre szolgál a `teszt`. Hiányzó implementáció, hozzáférés vagy futtatási előfeltétel viszont nem nevezhető kész felhasználói tesztverziónak.
- A tesztkiadáshoz dokumentáld a forrás `dev` commitot és az ellenőrzések eredményét. A felhasználónak jelezd az ágat és a tesztelés módját.
- A `main` ágra nem kerül automatikusan fejlesztés ettől a munkarendtől; most nincs feladat annak frissítésére.

## Együttműködés és adatvédelem

- Módosítás előtt frissítsd és ellenőrizd a távoli ágakat. Párhuzamos asztali Work-fejlesztés is lehet folyamatban; a nem pusholt változásokat nem látod.
- Ne használj force push-t és ne írd felül más munkáját. Távoli ág változásakor olvasd újra, majd szabályosan fésüld össze a saját változtatásaidat.
- A meglévő személyes `.local/` profilt, adatbázist, fényképeket és Docker-köteteket őrizd meg. Meglévő Flyway-migrációt ne módosíts.
- Olvasd el az aktuális feladatot és a közös promptutasításokat. A fenti, felhasználó által megadott ágmunkarend elsőbbséget élvez a régebbi, commit/push előtti megerősítésre vonatkozó szöveggel szemben.
- A beszámolóban különítsd el az elkészült, ellenőrzött és még felhasználói tesztre váró részeket.
