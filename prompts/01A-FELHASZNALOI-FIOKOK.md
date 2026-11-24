# 01A – Alkalmazásfiókok és bejelentkezés

> **Státusz:** Fejlesztés alatt  
> **Pontosítás / akadály:** A fiókok és a bejelentkezés dev-megvalósítása elkészült. A kanonikus Cloudflare-es útvonalon az alkalmazásbelépés és a valódi iPhone-próba még dokumentálandó. A natív Windows-fiókkarbantartás és a Docker külön környezeti próba.
> **Megvalósítás:** dev, [`815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c`](https://github.com/borosdavid/borosCode/commit/815dcb2cacd55b5477ae8066fa1c7d48c4b6a83c).  
> **Ellenőrzés:** 2026-11-18: dev [`f3c2f74`](https://github.com/borosdavid/borosCode/commit/f3c2f742fd57b4633a226f880d74685984549d1c), 18 egységteszt, 16 integrációs teszt MySQL 8.4.8-cal és 4 headless böngészős tesztcsoport sikeres. A V2 sémából meglévő tesztadatokkal sikerült V3/V4-re migrálni. Az első fiók létrehozása a becsomagolt JAR interaktív parancsával, majd az első belépés és a fotó megőrzése újraindítás után sikeres külön Linux/MySQL 8.4.8 sémán. [Automatizált próba](https://github.com/borosdavid/borosCode/commit/be040d584e1a26a10bfbc933ed5a2e53c43106db).
> **Felhasználói teszt:** Nincs tesztágas átadás vagy elfogadás; a kötelező környezeti próbákig a prompt aktív marad.  
> **Frissítve:** 2026-11-24. [Státuszszabályok](../projekt/STATUSZOK.md).

## Cél és kapcsolódás

A [D-008 döntés](../projekt/DONTESI-NAPLO.md) alapján minden alkalmazáshasználó saját felhasználónévvel/jelszóval lépjen be. Kezdetben egy közös alkalmazásszintű jogosultság van: minden aktív, bejelentkezett fiók minden modulhoz és a közös adatokhoz hozzáfér. Nincs felhasználónként elkülönített leltár vagy új moduljogosultság-rendszer. Az alkalmazásfiók nem SSH-, Linux-, GitHub- vagy szerveradminisztrációs fiók.

Ez az 01-es PWA-fázis hiányzó hozzáférésvédelmi részének körülhatárolt feladata. A 00A/JPA tényleges állapotát előbb ellenőrizd; a biztonságos telefonos 01 átadás, illetve a külön Windows 01B kör feltétele az itt leírt belépés elkészülte. A fájl létezése önmagában nem indítja el az implementációt.

## Megvalósítási irány

- Az aktuális Spring Boothoz illő Spring Security, szerveroldali sessionnel; a böngésző ne localStorage-ban tárolt hosszú életű JWT-vel vagy eltett jelszóval jelentkezzen be.
- Egyedi, stabil belső felhasználóazonosító, egyértelmű felhasználónév-normalizálás, sózott adaptív password hash, aktív/letiltott állapot. Használj megfelelő PasswordEncoder-t, a költséget a hardveren mérd; nincs sima szöveges vagy visszafejthető jelszótárolás.
- Bejelentkezés/kijelentkezés, sessionazonosító-csere belépéskor, lejárat és letiltás utáni online érvénytelenítés; HTTPS-en Secure/HttpOnly/SameSite cookie és valódi CSRF-védelem a form/multipart/API írásokra is.
- A jelenlegi X-Otthontar-Request fejléc nem felhasználóhitelesítés és önmagában nem session-CSRF-token. A régi védelmet szabályosan illeszd az újhoz.
- Védett leltároldalak, minden érzékeny API, eredeti képek és előnézetek. Csak a belépőoldal és a szükséges személyes adat nélküli statikus PWA-erőforrások publikusak az alkalmazáson belül.
- API-n lejárt/nem létező session: kezelhető 401 JSON-válasz; ne kapjon a mentő API HTML-belépőoldalt sikeres adatként. A megnyitott piszkozat és fotók maradjanak meg újrabelépésig.
- Hibás belépésre általános hibaüzenet, belépési próbálkozások korlátozása; jelszó/session/token nem naplózható.
- Első fiók létrehozása és további fiókok felvétele kezdetben helyi, védett karbantartói folyamattal; nincs alapjelszó és nyitott önregisztráció. Ez induló technikai alapértelmezés, nem egy rejtett alkalmazásszerepkör. Ha később alkalmazásbeli fiókkezelő felület kell, az egyenlő jogok és az új fiókok létrehozásának üzleti szabálya külön pontosítandó.
- Új Flyway-migráció, a meglévő adatok és fotók megtartásával. A fiókadatok az adatbázismentésbe kerülnek; titok nem kerül Gitbe.
- A localhost/host/proxy szűrés új modellje csak megbízható proxyból származó fejléceket fogadjon el, meghatározott HTTPS hosttal. Ne állítsd át általános proxybizalomra.
- A közös adatokhoz való azonos hozzáférés nem indok a személyes adatok vagy nyers naplók vezérlőrepóba másolására.

## Offline és több fiók ugyanazon eszközön

Az első bejelentkezés és aktiválás online történik. A 02/03-as offline fázisok szerződésében:

- Korábban engedélyezett helyi offline adatok böngészése hálózat nélkül is lehetséges; nincs helyben eltett jelszóval „szerverbelépés”.
- Az online session lejárata/tiltása szinkron előtt újrabelépést igényel. Offline kliensnél azonnali távoli visszavonás nem garantálható; ezt a használati útmutató mondja ki.
- Kijelentkezés és fiókváltás ne hagyjon másik fióknak hozzáférhető korábbi helyi cache-t. Szinkronizálatlan módosítás/fotó esetén előbb megőrzés/export vagy felhasználói döntés kell; az outboxot nem szabad csendben törölni vagy másik fiók nevében automatikusan feltölteni.
- A cache/metaadatok felhasználóazonosítóhoz kötődjenek; a közös szerveres adatok ettől még közösek maradnak.
- A Service Worker nem cache-el belépési/session-választ vagy védett API-t véletlenül. A 02/03 tudatos offline adatkezelése külön ellenőrzendő.

## Elfogadási feltételek

1. Két tesztfiók ugyanazt a közös leltárt és funkciókat éri el; belépés nélkül egyik érzékeny endpoint/kép sem olvasható.
2. Helyes/hibás belépés, próbálkozáskorlát, kijelentkezés, sessionlejárat és letiltás tesztelve; nincs eltérő modulszerepkör.
3. Session fixation és CSRF próbák; multipart és desktop műveletek is védettek. Jelszó csak megfelelő hashként kerül adatbázisba.
4. 401 után a nyitott piszkozat/fotó megmarad, újrabelépés után szabályosan menthető.
5. A `https://boroscode.com/otthontar/` címen asztali és iPhone Safari böngészőpróba. Ellenőrizd az alútvonalhoz tartozó munkamenetet, a belépés utáni visszairányítást, a kijelentkezést, valamint a tiltott hostok és a jogosulatlan közvetlen háttérhost-kérések elutasítását.
6. Az offline cache/fiókváltás követelményeit a 02/03-as feladatokkal összehangolt tesztszerződés rögzíti; még nem elkészült offline funkcióról nem állítunk teljes tesztet.
7. Automatizált releváns ellenőrzések és kitöltött átadási leírás; éles személyes profil érintetlen.

Források: [Spring Security Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html), [session persistence](https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html), [CSRF](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html).
