# Otthontár 01G – Leltári események és kézi próba

A naptár a dátummal rögzített leltári eladásokat és a 01G óta végrehajtott
költöztetéseket is mutatja. A kapcsolt tétel a terv részleteiben látszik. Ha az
esemény és a tétel más napra esik, a tétel a saját napján is megtalálható. A
csoportos terv a **Megtörtént** művelet és a mentés után zárul le.

## Elkülönített helyi indítás

Java 21-es builddel vagy az alábbi Docker Compose profillal indul a fejlesztői
próba. Az utasítás tiszta munkafát és Docker Compose-t feltételez. A
`.env.01g` helyi fájlba az `.env.example` alapján írj külön próba-jelszavakat
a `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `READER_PASSWORD` változókhoz.
Legyen `WEB_PORT=18749`, `DB_PORT=43319` és `SESSION_COOKIE_SECURE=false`.
Őrizd a fájlt a Git-en kívül.

```bash
git fetch origin dev
git worktree add --detach ../otthontar-01g-proba origin/dev
cd ../otthontar-01g-proba
docker compose --env-file .env.01g -p otthontar-01g-proba up -d --build
docker compose --env-file .env.01g -p otthontar-01g-proba exec inventory \
  java -jar /app.jar account create tesztelo
```

Nyisd meg a [helyi naptárat](http://127.0.0.1:18749/leltar/naptar) a létrehozott
fiókkal. Az elkülönített `otthontar-01g-proba` Compose-projekt saját DB- és
fotóköteteket használ. Leállításhoz futtasd a fenti Compose-beállításokkal
a `stop` parancsot. A kiadáshoz a pontos commitot rögzítő
[tesztverzió-eljárás](https://github.com/borosdavid/borosCode/blob/main/projekt/TESZTVERZIOK.md)
érvényes.

A telefonos próba külön, védett HTTPS-en telepített tesztpéldányt igényel.
Azonos tesztadatsorral futtasd a próbákat asztali böngészőben, iPhone Safariban,
majd a kezdőképernyőről megnyitott alkalmazásban. Ellenőrizd, hogy a
bejelentkezés, az alkalmazás útvonalai és a naptár statikus erőforrásai is a
tesztpéldány címét használják. A próba idején szükséges az internetkapcsolat.

## Adatok és próbalépések

Helyszínek: Lakás, Raktár, Új lakás. Tárgyak: Lámpa és Asztal a Lakásban,
Polc és Kisasztal a Raktárban. Az összes példa a tesztpéldány adataival fut.

1. Hozz létre egy háromnapos **Költözés** tervet a Lámpával és az Asztallal.
   Ütemezd át a három napot. A Lámpa adatlapján változtasd a helyét Raktárra.
   Az első mentéskor a párbeszédben ellenőrizd a terv nevét és a két helyet.
   Lépj vissza a szerkesztésbe, és ellenőrizd, hogy a bevitt mezők megmaradtak.
   Másik lapon ellenőrizd a mentett adatlapot és tervet. Ments újra és hagyd
   jóvá. Csak a Lámpa kiindulási helye változzon a tervben.
2. Rendelj a Lámpához és az Asztalhoz **Költöztetés** műveletet és Új lakás
   célhelyet. Hajtsd végre a két költöztetést egyenként, az egyiket a
   Költöztetés listában, a másikat a tárgy adatlapján. A naptárban jelenjen
   meg mindkét tényleges nap és helyszín. A terv maradjon nyitott. Zárd le a
   **Megtörtént** gombbal, tényleges kezdő- és végdátummal.
3. A Polchoz készíts két nyitott költözési tervet, majd hajts végre egy
   leltári költöztetést. Az önálló sor címkéje **Leltárból**. Nyisd meg,
   és válaszd ki az egyik tervet az **Összekapcsolás** művelettel. Azonos
   napon a sor a terv részeként látszik. Másik napra áthelyezett terv esetén
   a tényleges tétel továbbra is szerepel a saját napján. Ismert dátumú
   korábbi költözés kézzel is felvehető naptáreseményként.
4. A Kisasztalhoz hozz létre **Eladás** tervet. Az Eladás listán az **Eladva**
   gombbal rögzíts árat és napot. Ellenőrizd a kapcsolatot és a terv nyitott
   állapotát. Az adatlapon írd át az eladás dátumát, majd állítsd vissza a
   tárgyat Meghirdetve állapotra. A dátumjavítás áthelyezi a tételt.
   Visszavonás után a terv részleteiben olvasható a változás. Végezd el
   ugyanezt egy próbatárgy közvetlen, **Elkelt** állapotú adatlapos mentésével.
5. Mondj le egy próbaeseményt, majd keresd meg a **Lemondott** szűrővel.
   A végén ellenőrizd a Tárgyak, az Eladás és a Költöztetés listáját.

## A 02-es fázisnak átadott adatszerződés

| Adat | Azonosító és verzió | Forrás |
|---|---|---|
| Kezelt naptáresemény | UUID és optimista `version` | `source=CALENDAR`, `GET /api/calendar/{id}` |
| Leltári eladás | A tárgy UUID-jából képzett stabil, névterezett UUID, a tárgy `version` értéke | `source=INVENTORY`, a tárgy `SOLD` állapota és `sold_date` mezője |
| Végrehajtott költöztetés | Műveletenként saját UUID, változatlan végrehajtási adat, `version=0` | `source=INVENTORY`, `calendar_inventory_fact` és budapesti naptári nap |

A dátumformátum `YYYY-MM-DD`. Az időszak kezdete beleértendő, a vége kizáró.
A tényleges tételek a `facts` tömbben találhatók. A `date` a leltári nap,
az `eventId` a hozzárendelés, a `withdrawn` a visszavont vagy dátum nélkül
maradt kapcsolt eladást jelzi. A mintához tartozik a `GET /api/calendar/facts/{id}`,
a `GET /api/calendar/facts/{id}/candidates` és a
`PUT /api/calendar/facts/{id}/assignment`. A mentés ellenőrzi a terv és a
forrás verzióját. Egy tétel egy eseményhez kapcsolható. Egy esemény több tényleges
tételt fogadhat. A tényleges sorok szűrhetők típus, tárgy, hely és **Megtörtént**
állapot szerint.

A tárgymódosítás `CALENDAR_LOCATION_APPROVAL` kódú HTTP 409 válasszal adja vissza
a régi és az új helyet, az érintett nyitott eseményeket és azok verzióit. Az
új multipart kérésben a változatlan tárgyadatok mellé a `calendarApproval`
JSON-rész kerül. Az adatlapról indított **Áthelyezés végrehajtása** a
`draft.executeMove=true` jelzőt küldi. A naptár offline működéséről a 02/03
pontosításakor külön döntés születik.
