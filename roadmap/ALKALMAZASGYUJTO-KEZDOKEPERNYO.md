# Közös alkalmazásgyűjtő és csempés kezdőképernyő

> **Státusz:** Tervezett  
> **Pontosítás / akadály:** A részletes cél a dokumentumban szerepel; a megvalósítás még nincs igazolva.  
> **Megvalósítás:** Nincs igazolt megvalósítási commit.  
> **Ellenőrzés:** A terv/feladatleírás létezése ellenőrizve; implementációs teszt nincs igazolva.  
> **Felhasználói teszt:** Nincs átadva; elfogadás nincs igazolva.  
> **Frissítve:** 2026-11-17. [Státuszszabályok](../projekt/STATUSZOK.md).


**Felületek:** web, iOS és Android, közös szerveroldali háttérrel
**Rögzítve:** 2026-11-17

## Cél

A meglévő backend–web–iOS–Android megoldás hosszabb távon különböző funkciók közös gyűjtőalkalmazásává bővüljön. Az Otthontár leltár ennek egyik önálló modulja legyen. A gyűjtőalkalmazás végleges neve még nyitott.

Weben és az alkalmazásokban egyaránt csempés kezdőképernyő legyen az alapértelmezett induló felület. Innen lehessen belépni a kiválasztott funkcióba, majd egyszerűen visszatérni a közös kezdőképernyőre.

## Kezdeti csempék

| Csempe | Funkció |
|---|---|
| Otthontár | Meglévő tárgy-, költözési és eladási leltár |
| Naptár | A tervezett naptárfunkció, kezdetben költözési, eladási és átadási eseményekkel |
| Sport- és táplálkozásnapló | Edzések és étkezések különálló modulja |
| Snake | Helyben futó játék, közös szerveres ranglistával |
| Későbbi funkciók | Új, önálló modulok a felhasználó további igényei alapján |

Kapcsolódó tervek: [naptár](KOLTOZESI-NAPTAR.md), [sport és táplálkozás](SPORT-ES-TAPLALKOZAS.md), [Snake](SNAKE.md), [asztali felület](ASZTALI-FELULET.md), [Android](ANDROID.md).

## Elvárt működés

- Ugyanazok a modulok legyenek felismerhetőek weben, iOS-en és Androidon, az adott képernyőmérethez és beviteli módhoz igazítva.
- A csempékből a megfelelő modul nyíljon meg; a moduloknak maradjon saját útvonala és közvetlen megnyithatósága.
- A meglévő Otthontár-adatok, könyvjelzők és munkafolyamatok megmaradjanak. A mostani PWA start_url/scope módosítását és a meglévő telepítések frissítését külön meg kell tervezni.
- A kezdőképernyő és a már telepített helyi funkciók offline is elérhetők legyenek. Egy szerverkimaradás ne akadályozza például a Snake elindítását.
- Az egyes csempék a modul saját elérhetőségét tükrözzék. A helyi játék működhet akkor is, amikor a szerveres ranglista nem tölthető be.
- Új modul felvétele legyen következetes: csempe, cím, ikon, útvonal, elérhetőségi és offline szabályok. Ne kelljen minden új funkciónál újratervezni a teljes navigációt.
- Érintéses, billentyűzetes és akadálymentes használat; a csempe szövegesen is azonosítható legyen.
- A még el nem készült funkciókról ne állítsuk, hogy működnek. Megjelenítésük vagy elrejtésük a későbbi felülettervezés része.

## Modulhatárok és közös elemek

Közös lehet a kezdőképernyő, a navigáció, az arculat és – ahol szükséges – a hitelesítés. A leltár, a naptár, a sportnapló és a játék külön funkcionális modulként maradjon kezelhető, saját adatokkal és API-felelősséggel. A közös háttérrendszer nem jelenti azt, hogy minden funkciónak folyamatos szerverkapcsolat kell.

A modulokra bontás nem jelent automatikusan külön mikroszolgáltatásokat, új frontend-keretrendszert vagy natív appokra váltást. Ezeket a részletes tervezéskor, a tényleges igények alapján kell eldönteni.

## Nyitott döntések

- A gyűjtőalkalmazás neve és a meglévő Otthontár név helye az arculatban.
- Csempék sorrendje, testreszabhatósága és a még fejlesztés alatt álló modulok megjelenése.
- A külön Naptár modul és a sportnapló saját naptárnézetének kapcsolata; a költözési naptár meglévő célja maradjon meg.
- Közös felhasználó-/háztartáskezelés és modulonkénti jogosultságok.
- Web/PWA és későbbi iOS/Android csomagolás közös navigációs és frissítési szerződése.

## Megvalósítási irány

Először a közös kezdőképernyő és az Otthontár modulba történő belépés készülhet el, a meglévő működés megőrzésével. A többi csempe a hozzá tartozó modul elkészültével válhat használhatóvá. A pontos sorrend még nincs véglegesítve; ez a roadmap-bővítés nem indít el fejlesztést, és nem váltja fel a jelenlegi Windows-hozzáférési feladatot.

**Ez roadmap-dokumentum, nem végrehajtandó Codex-prompt.**
