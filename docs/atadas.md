# Windowsos átadás · 2026. november 13.

A natív Windows-indítást, a két Java-szolgáltatást és a MySQL 9.7.4-et a tényleges gépen ellenőriztük. Az elkülönített tesztek az acceptance adatkönyvtárban futottak. A személyes profil induláskor nulla tárgyat tartalmazott.

| Ellenőrzés | Eredmény |
|---|---|
| Maven build, Java-formázás ellenőrzése, 3 JUnit-próba | Sikeres |
| Három JPG előnézete, középső kivétele, két kép mentése, eredeti bájtok és borítóváltás | Sikeres |
| Irányár, tényleges ár, dátum, link, eladási sorrend és közös helyszínszűrő | Sikeres |
| Helyszín átnevezése, negyedik helyszín, áthelyezés, újraindítás utáni megőrzés | Sikeres |
| Lomtár újraindítás után, törlés megszakítása, visszaállítás és a képfájlok végleges eltávolítása | Sikeres |
| Két böngészőlap ütközése, megtartott szöveg és fotó, saját értékek mentése | Sikeres |
| Navigáció és lapbezárás szerkesztéskor, maradás, az eredeti mező visszaírása utáni kilépés | Sikeres |
| Hamis JPG elutasítása, megszakadt kérés újrapróbálása | Sikeres |
| VS Code SQLTools 0.28.6 és MySQL-meghajtó 0.6.7 telepítése, lekérdezés a meghajtón keresztül | Sikeres |
| Személyes adatbázis viewer jogosultságai | SELECT az inventory sémán |
| Docker Compose futtatása | A gépen nincs Docker. A működő átadás a natív Windows-profil. |

A böngészős próbák az Edge motorját és a futó MySQL-t használták. A tábla megnyitásának VS Code-kezelőfelületi lépéseit a README ismerteti. A Docker-útvonal külön, friss gépen ellenőrizhető a README Windowsos Docker-beállításával és az Inditas.cmd indításával.
