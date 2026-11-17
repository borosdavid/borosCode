# Tesztverziók és átadás

Hiteles eljárásrend: `main`. A kiadáshoz tartozó kitöltött tesztútmutató a kóddal együtt, a `dev` → `teszt` útvonalon halad. Ez az eljárás nem módosítja a [kiadási feltételeket](../docs/branch-workflow.md).

## Verzióazonosító

Minden új, felhasználónak átadott tesztkiadás kapjon **annotált Git-címkét**:

`teszt-ÉÉÉÉ.HH.NN.SORSZÁM`

Példaformátum: `teszt-2026.11.17.1`. Ez csak példa, jelenleg nem létező kiadás.

- A dátum a magyarországi átadás napja, a sorszám naponta 1-ről indul.
- Az azonosító a tesztkiadást jelöli; nem változtatja meg automatikusan a Maven-verziót, a PWA cache-verzióját vagy a végleges alkalmazásverziót.
- A címke a `teszt` ág **pontos, átadott commitjára** mutasson. A `teszt` ág később mozoghat, a kiadási címkét nem mozgatjuk és nem használjuk újra.
- Javításkor új sorszám és új átadási lap kell. A korábbi lap és tag megmarad; a lapot a későbbi commitban „felváltotta” megjegyzéssel lehet kiegészíteni.

## Kötelező átadási lap

Használd a [tesztátadási sablont](sablonok/TESZTATADAS.md). A kitöltött példány helye: `docs/tesztkiadasok/<verzioazonosito>.md`.

Tartalma: rövid változáslista, forrás dev commit, felhasznált main prompt-commit, ellenőrzött kód és teszteredmények, futtatás/elérési cím, adatizoláció, kézi tesztlépések és elvárt eredmények, korlátok, visszajelzés módja és visszaállítási terv. A végleges kiadás pontos commitját a tag adja meg; a saját commitazonosítót nem kell önhivatkozóan a fájlba írni.

A tesztkiadás nem felhasználói elfogadás. Az elfogadás dátumát és visszajelzését utólag a központi feladatlap és projektállapot rögzíti; a kiadási tag változatlan marad.

## Átadási folyamat

1. Frissítsd a `main`, `dev`, `teszt` távoli ágakat és a címkéket. Vizsgáld meg az ágak eltéréseit és a már létező verzióazonosítókat.
2. Jelöld ki a kész feladatkört és a forrás dev commitot. Ellenőrizd a felhasználói próba előfeltételeit; hiányzó HTTPS vagy félkész funkció nem átadható.
3. A `dev` ágon töltsd ki és commitold/pushold az átadási lapot. A forrás kódcommit és a tesztjelentés legyen rögzített; a dokumentációs commit nem helyettesít tesztet.
4. Izolált munkafán készítsd elő a `teszt` ágra emelést. Fast-forward, válogatott commitok vagy szabályos merge választható az eltérések ismeretében; ne vigyél át későbbi, félkész kódot. Ne cseréld le a main központi terveit régebbi ági másolatokra.
5. Az előkészített **végleges tesztjelölt** kódján végezd el a releváns ellenőrzéseket. Ha a megvalósítást érintő konfliktust oldottál fel vagy megváltozott a kód, a korábbi teszteredmény önmagában nem elég. A tagelt állapotban már legyen benne a kitöltött átadási lap.
6. Hozd létre az annotált címkét a pontos tesztcommitra. A tag üzenete tartalmazza a verziót, a forrás dev commitot és az átadási lap útvonalát.
7. A `teszt` ág és a címke együtt, lehetőleg egyetlen atomikus push-sal kerüljön fel. Nem atomikus környezetben külön ellenőrizd mindkettőt; részleges siker esetén ne állíts kész átadást.
8. Távolról ellenőrizd a tag által feloldott commitot és az átadási lapot. A main projektállapotában a feladat legyen Tesztelhető, tag- és útmutató-hivatkozással.
9. A felhasználónak add meg a verziót, a belépés/indítás módját és a konkrét próbákat. Ha a jogosultság rendben van és már kérte az átadást, ne kérj újabb rutinszerű commit/push-megerősítést.

### Git-parancsok az előkészített kiadás jelöléséhez

A következő Bash-példa egy már ellenőrzött, tiszta munkafát és helyi `teszt` ágat feltételez. A változókat tényleges értékekkel töltsd ki; a folyamat nem teszteli és nem emeli át automatikusan az alkalmazást.

```bash
set -euo pipefail

git fetch origin main dev teszt --tags
test_version='teszt-ÉÉÉÉ.HH.NN.1'
source_dev_sha='TELJES_ELLENORZOTT_DEV_COMMIT'
test_commit=$(git rev-parse 'teszt^{commit}')
test_note="docs/tesztkiadasok/$test_version.md"

git show "$test_commit:$test_note"
git tag -a "$test_version" "$test_commit" \
  -m "$test_version; source-dev=$source_dev_sha; guide=$test_note"
git push --atomic origin refs/heads/teszt "refs/tags/$test_version"
git ls-remote origin refs/heads/teszt "refs/tags/$test_version" "refs/tags/$test_version^{}"
```

Annotált tagnél a nyers tag objektum SHA-ja eltér a commit SHA-jától; a `^{}` végű, feloldott távoli hivatkozást hasonlítsd a tesztcommithoz. Sikertelen vagy ütköző push után vizsgáld meg a távoli állapotot; ne használj force push-t, és ne törölj/írj át kiadott címkét.

## Jelenlegi helyzet – 2026-11-17

A GitHub tags és releases lekérdezése üres eredményt adott. A jelenlegi PWA még nem teljesíti a telefonos átadás feltételeit, ezért **most nem hoztunk létre tesztkiadási címkét**. A keretrend és a sablon készen áll a következő valódi átadáshoz. A munkarendi dokumentáció változásai nem alkalmazás-tesztkiadások.
