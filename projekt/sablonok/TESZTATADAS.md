# [teszt-ÉÉÉÉ.HH.NN.SORSZÁM] – [feladat rövid címe]

Ez sablon; kitöltve a `docs/tesztkiadasok/<verzio>.md` útvonalra kerül a `dev` ágon, majd a kóddal együtt a `teszt` ágra. A helyőrzőket kiadás előtt cseréld ki.

| Adat | Érték |
|---|---|
| Verzió / Git-tag | [pontos azonosító] |
| Átadás dátuma | [magyarországi dátum] |
| Feladat | [main prompt/roadmap állandó hivatkozás] |
| Forrás dev commit | [teljes SHA és hivatkozás] |
| Felhasznált main prompt-commit | [teljes SHA és hivatkozás] |
| Tesztelt kód / jelölt commit | [teljes SHA; a végleges tagelt kód ellenőrzésének bizonyítéka] |
| Végleges kiadási commit | [a Git-tag által rögzítve; a közzétételkor ellenőrizendő] |
| Korábbi tesztverzió | [címke vagy nincs] |
| Környezet | [operációs rendszer, futtatás, kliens és szükséges verziók] |

## Változáslista

- [Mi változott a felhasználó számára?]
- [Mi a kijelölt, kipróbálható feladatkör?]

## Ellenőrzések

| Ellenőrzés | Ellenőrzött commit | Dátum | Eredmény / riport |
|---|---|---|---|
| [releváns build/teszt/parancs] | [SHA] | [dátum] | [siker/hiba/nem futott és oka; bizonyíték] |

[Külön jelezd, mi nem futott, illetve mi származik korábbi riportból. Merge-konfliktus vagy kódváltozás után a végleges kódot újra ellenőrizd.]

## Elérés és indítás

[Konkrét HTTPS-cím vagy indítási parancs; hozzáférési feltételek és a verzió ellenőrzésének módja. Jelszó és token ne kerüljön ide.]

## Adatok és előfeltételek

[Külön tesztadatbázis és képtároló, jogosultság, szükséges készülék. Személyes/éles adatok védelme, szükséges mentés helye titkok nélkül.]

## Kézi próbák

| Lépés | Művelet | Elvárt eredmény |
|---|---|---|
| 1 | [konkrét felhasználói művelet] | [ellenőrizhető eredmény] |
| 2 | [hibaág vagy megszakítás, ha releváns] | [elvárt adatmegőrzés/hiba] |

## Ismert korlátok

[Mi maradt ki a kijelölt scope-ból? Kötelező előfeltétel hiányát ne kezeld pusztán korlátként.]

## Visszajelzés és elfogadás

A visszajelzésben szerepeljen a verzióazonosító, eszköz/böngésző, elvégzett lépés és tényleges eredmény. A felhasználó itt a beszélgetésben is jelezheti; nincs szükség telefonhívásra. Az elfogadást és a hátralévő javításokat a main központi feladatlapjába vezessük át.

## Visszaállítás

[Korábbi ismert jó verzió, visszaállítási lépések, adatbázis-migráció és fotók kompatibilitása. A kód visszaváltása önmagában nem adat-visszaállítás; ne törölj személyes profilt vagy Docker-kötetet.]
