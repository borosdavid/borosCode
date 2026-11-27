# Otthontár naptár – 01F kézi próba

A **Naptár** menüpont a leltár része. A havi ábra és a lista ugyanazokat az
eseményeket mutatja. A listában egy többnapos esemény egy sor. Az eseményeket
típus, helyszín, tárgy és állapot szerint lehet szűrni. Keskeny képernyőn lista,
asztali szélességen hónapnézet nyílik meg alapból.

## Asztali és iPhone-próba

A 01F-et tartalmazó, V5 migrációval indított példányon jelentkezz be az
Otthontárba. A helyi cím `/leltar/naptar`, telepítési előtaggal
`/otthontar/leltar/naptar`. iPhone-on nyisd meg a telepített változat HTTPS-címét
Safari-lapon, majd ismételd meg a próbát a főképernyőről indított alkalmazásban.

1. Két mentett próbatárggyal a naptárban hozz létre háromnapos költözést,
   amely a hónap utolsó napján vagy azelőtt indul, és a következő hónapban
   ér véget. Adj eltérő kiindulási helyet a tárgyakhoz. Ellenőrizd mindkét
   hónapot és a listanézetet.
2. Nyisd meg az egyik társított tárgy adatlapját a naptárból. Az adatlapon
   válaszd az **Új naptáresemény ehhez a tárgyhoz** linket. Az űrlapon ellenőrizd
   az előre kiválasztott tárgyat és annak jelenlegi helyét.
3. Ütemezd át a költözést, majd kattints a **Megtörtént** gombra. Töltsd ki a
   tényleges kezdő- és zárónapot, és mentsd. A részletekben a tervezett dátumok
   is visszanézhetők, a naptár a tényleges időszakot mutatja.
4. Hozz létre átfedő eladási és költözési tervet ugyanarra a tárgyra. A mentés
   után a részletekben jelenjen meg figyelmeztetés. Újabb nyitott eladási terv
   próbájánál nyisd meg az ütközésben felkínált meglévő tervet.
5. Próbáld ki az **Esemény lemondása** műveletet és mentsd. A **Lemondott**
   állapotszűrővel keresd meg a tervet. Egy elmúlt idejű nyitott terv a
   **Lejárt** jelzést kapja és átütemezhető marad.
6. Mentés előtt szakítsd meg a hálózatot. A kitöltött űrlap megmarad.
   Kapcsolat helyreállítása után ments újra. Két lapon történő szerkesztésnél
   a másodikként mentett, elavult módosítás ütközést jelez és megtartja a
   mezőket. A szerverváltozat linkje külön lapon nyílik meg.

## Kapcsolódó fejlesztés

A naptár saját fájljai a meglévő, továbbított `/leltar/assets/` útvonalon futnak.
Az API és a naptáradatok eléréséhez élő kapcsolat kell. A leltárműveletek
automatikus párosítása és a teljes felhasználói átadás a 01G része.
