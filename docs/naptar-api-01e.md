# Otthontár naptár API – 01E

A naptár az Otthontár hitelesített leltárához tartozik. A nyilvános telepítés
alkalmazás-előtagját az URL elé kell tenni. Az `/api/calendar` például az
`/otthontar/api/calendar` útvonalon érhető el a kanonikus szerveren. Az íráshoz
az alkalmazás szokásos CSRF-fejléce, `X-Otthontar-Request: 1` és az
`X-Otthontar-User` munkamenet-fejléc szükséges. Ezeket a meglévő `AccountAuth`
kezeli a böngészőben.

| Művelet | Kérés | Sikeres válasz |
|---|---|---|
| Lista | `GET /api/calendar?from=2026-11-01&until=2026-12-01` | `200`, események tömbje |
| Részletek | `GET /api/calendar/{id}` | `200`, az esemény és dátumütközései |
| Létrehozás | `POST /api/calendar` JSON | `201`, az esemény aktuális verziója |
| Módosítás | `PUT /api/calendar/{id}` JSON | `200`, a mentett esemény aktuális verziója |

A listához a `from` és az `until` kötelező. A lekérdezési intervallum 1–366 nap,
az alsó határ inkluzív, a felső exkluzív. Egy többnapos esemény szerepel az
eredményben, ha legalább egy napja beleesik az intervallumba. A rendezés az
esemény kezdőnapja, majd az azonosító alapján történik. A `type`, `status`,
`locationId` és `itemId` opcionális szűrők. A helyszínszűrő az esemény helyét és
a kapcsolt tárgyak kiindulási helyét is figyelembe veszi.

## Kérés és válasz

Létrehozási példa, előzetesen kiválasztott leltári tárggyal és helyszínnel.

```json
{
  "id": "279a6d44-2d14-4788-9567-cc80613429da",
  "title": "Költöztetés",
  "type": "MOVE",
  "status": "PLANNED",
  "notes": "Dobozok előkészítése",
  "locationId": null,
  "plannedStart": "2026-11-27",
  "plannedEnd": "2026-11-29",
  "items": [
    {
      "itemId": "9aece6a1-1476-4121-b3ec-f3d72a3cba84",
      "originLocationId": "8204f501-e697-4cb8-8553-c067ca79821f"
    }
  ]
}
```

- Típusok `MOVE`, `SALE`, `TRANSPORT`, `HANDOVER`, `PICKUP`, `OTHER`.
  Állapotok `PLANNED`, `DONE`, `CANCELLED`.
- A kliens létrehozásonként stabil UUID-t állít elő. A létrehozásnál a
  `version` hiányzik, módosításnál a legfrissebb válaszból kötelező megadni.
  A `PUT` az egész eseményt és teljes tárgylistáját küldi. A `items` hiánya
  üres tárgylistát jelent. Válaszvesztés után a kliens az azonosítóval tudja
  lekérni a létrejött eseményt. Az ismételt azonosító `409` választ ad.
- A cím 1–200, a megjegyzés legfeljebb 2000 karakteres. A tárgylista legfeljebb
  100 különböző elemet tartalmazhat. A helyszínek opcionálisak.
- A dátumok `YYYY-MM-DD` formátumúak. Az esemény kezdő- és zárónapja a
  tartomány része. Egy napra elég a kezdődátum, a válaszban a záródátum
  ugyanaz lesz. A megtörtént esemény a tényleges időszakára listázódik.
  Többnapos terv lezárásakor az `actualStart` és az `actualEnd` is kötelező.
  Terv nélkül rögzített történeti eseményhez elég a tényleges időszak.
- A válasz a normalizált mezőkön kívül a `version`, az `overdue` és a
  `warnings` mezőt is tartalmazza. Az `overdue` értéke a nyitott terv zárónapja
  után lesz igaz a budapesti naptári nap szerint, miközben az állapot
  `PLANNED` marad. A részlet- és mentési válaszban a `SALE_MOVE_OVERLAP`
  figyelmeztetés `eventIds` mezője az egymással átfedő tervek azonosítóit adja.
  A listaválaszban a `warnings` üres.
- Tárgyanként egy nyitott eladási terv menthető. Lezárással, lemondással vagy
  a tárgy eltávolításával felszabadul a helye. A leltárból végleg törölt
  tárgy kapcsolata megszűnik. A törölt helyszínhez tartozó hivatkozás `null`
  lesz, az esemény és dátumai visszakereshetők.

A `400` hibás kérésformátumot, a `401` hiányzó munkamenetet, a `403`
sikertelen jogosultsági ellenőrzést jelöl. Ismeretlen eseményazonosító
`404`. A `409` elavult verziót vagy egyediségi ütközést jelez, és a szerveren
az utolsó sikeres mentés marad. A `422` üzleti validációs hiba, például
fordított dátumtartomány vagy ismeretlen tárgy.

Második nyitott eladási terv mentésekor a `409` válasz `conflictingEvents`
mezője tartalmazza a meglévő tervek `id` és `title` mezőit. A felület ebből
nyitja meg az eredeti tervet.

## Átadás a következő fázishoz

A `V5__calendar.sql` migráció két új táblát ad hozzá. A naptári menü és űrlap
a 01F feladata. A megtörtént leltárműveletek párosítása és a tárgyhely
módosításának jóváhagyása a 01G feladata. A teljes online naptár felhasználói
átadása a 01G után esedékes.
