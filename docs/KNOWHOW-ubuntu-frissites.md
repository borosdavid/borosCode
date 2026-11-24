# Az Ubuntu Otthontár-szerver frissítése telefonról

Az Ubuntu laptopon az `apollo` felhasználó futtatja az Otthontárat. Távoli frissítéshez
lépj be a laptopra a telefon SSH-klienséből. Ehhez előzetesen működő privát
SSH-elérés kell, például Tailscale SSH. A Cloudflare Tunnel a weboldalt szolgálja ki.

## Első használat

Az Ubuntu laptopon frissítsd egyszer a helyi `dev` munkapéldányt, hogy megkapd a
frissítőparancsot. Ez a lépés önmagában nem indítja újra a szervert.

```bash
cd /home/apollo/otthontar-ubuntu-teszt/borosCode
git pull --ff-only origin dev
```

## Frissítés a GitHubról

Csak azt az ágat indítsd, amelyen már ott van a szerverre szánt változtatás.
A jelenlegi Ubuntu-példány alapértelmezése a `dev`.

```bash
/home/apollo/otthontar-ubuntu-teszt/borosCode/scripts/Update-Ubuntu.sh
```

Ha a telepítendő alkalmazásverzió már a `teszt` ágon van, add meg a `teszt`
argumentumot. Megszakadható telefonos kapcsolatnál az Ubuntu felhasználói
systemd önálló munkaként is futtathatja a parancsot.

```bash
systemd-run --user --unit=otthontar-update --collect \
  /home/apollo/otthontar-ubuntu-teszt/borosCode/scripts/Update-Ubuntu.sh teszt
journalctl --user -u otthontar-update.service -n 50 --no-pager
```

A parancs kiírja a telepített commitot. Ezután ellenőrizd a
`https://boroscode.com/otthontar/` belépőoldalát.

## Mi történik frissítéskor?

- A GitHubról letöltött commit külön Git-munkafába kerül. A Maven a helyi JDK-val
  és Maven-példánnyal fordít és tesztel. Sikertelen build esetén az alkalmazás
  tovább fut az eddigi JAR-okkal.
- Sikeres build után a két Java-szolgáltatás rövid időre megáll. A frissítő
  lecseréli a JAR-okat, elindítja a szolgáltatásokat, majd mindkét helyi
  állapotvégpontot ellenőrzi. Sikertelen indításnál visszateszi az előző JAR-okat.
- A MySQL, a fotómappa, a Cloudflare Tunnel és a helyi konfiguráció az adott
  Ubuntu-példányhoz tartozik. A JAR-visszaállítás nem vonja vissza a Flyway által
  már lefuttatott adatbázis-migrációkat. Új migrációt tartalmazó kiadás előtt
  készíts adatbázis-mentést.
