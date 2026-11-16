# Android-verzió az iOS Otthontár után

**Státusz:** tervezett, hosszabb távú
**Sorrend:** az iOS PWA offline-first használatának és szinkronizációjának stabilizálása után

## Cél
Az Otthontár teljes leltárának Android-telefonon történő használata, ugyanazzal a központi háztartási adatbázissal, mint a Windowsos böngésző és az iPhone.

## Várt funkciók
- Teljes leltár offline: minden tárgy, helyszín, állapot, és valamennyi fotó kis előnézete.
- Teljes felbontású kép online, igény szerint; új fotók eredetijének megőrzése feltöltésig.
- Androidos kamerás fotózás, offline adatfelvétel, módosítás, költöztetés, eladás/lomtár.
- Kétirányú, ütközésbiztos szinkron az iPhone-nal és az asztali böngészővel, közös backend API-val.
- Mobilos használhatóság és telepítési útmutató.

## Technológiai döntés – később
Először vizsgáljuk meg a már elkészült iOS PWA Androidon történő használhatóságát. Valódi natív Android-alkalmazás (pl. Kotlin/Jetpack Compose), Capacitor-wrapper vagy közös PWA csak akkor kerüljön kiválasztásra, ha az igények és a különbségek ismertek. Ne építsünk párhuzamos szerveroldali üzleti logikát.

## Függőségek és kockázatok
- A mobilos API, a szinkronprotokoll, a jogosultságkezelés és a biztonsági mentés legyen kiforrott.
- Android háttérfolyamatokra, fotóhozzáférésre és tárhelyhasználatra önálló ellenőrzés szükséges.
- Több kliens egyidejű offline módosításainak ütközéskezelését tesztelni kell.

**Ez roadmap-dokumentum, nem Codex-feladat.**
