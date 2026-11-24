# 01D – Otthontár napi, titkosított Ubuntu biztonsági mentése

**Implementáció:** `scripts/Backup-Ubuntu.sh`, `scripts/Verify-Backup-Ubuntu.sh`, `scripts/systemd/otthontar-backup.{service,timer}`, `scripts/tests/Backup-Ubuntu.Tests.sh`.  
**2026-11-24-i állapot:** az Ubuntu-példány helyi mentése üzembe helyezve. A telepített szkriptek forrása a `dev` ág [`95e6c4f`](https://github.com/borosdavid/borosCode/commit/95e6c4f) commitja. A közös projektutasítások forrása a `main` ág `5921afa` commitja.

## Az Ubuntu-példány üzembe helyezési naplója

- A két szkript külön felhasználói programkönyvtárba került (`~/.local/lib/otthontar/`). Az `age` 1.3.2 hivatalos kiadását SHA-256 ellenőrzéssel telepítettük ugyanott a `bin/` könyvtárba. A systemd szolgáltatás a meglévő, alkalmazás mellé telepített MySQL 9.7.4 klienst használja.
- A felhasználó a védett szerveren egyszerűbb kulcskezelést választott. A titkosított `.tar.age` formátum megmaradt. A kulcspár helyben készült, a privát kulcs a felhasználó privát konfigurációs könyvtárában van. A szerver vagy lemez elvesztése esetén a mentés és a kulcs együtt elveszhet. Az alábbi általános útmutató külön eszközön tárolt kulcsa választható telepítési mód.
- A mentés a meglévő helyi MySQL-admin opciófájlt használja a kapcsolódáshoz. Az Inventory és a MySQL helyi konfigurációja bekerül a titkosított archívumba. A mentés az alkalmazásfrissítés zárfájlját is használja.
- A kiesés nélküli `--check` sikeres. Az első éles mentés elkészült a `~/otthontar-backups/` könyvtárban, 600-as fájljogosultsággal. Az Inventory a mentés után aktív. A visszafejtés, a tartalomlista és a SHA-256 ellenőrzés sikeres.
- A visszaállítási próba külön, hálózati elérés nélküli MySQL-példányban futott. Mind a 8 tábla és minden táblánál a rekordszám egyezett. A forrás és a visszaállított fotómappa üres volt, ezért valódi fotófájlra nincs visszaállítási bizonyíték.
- A napi timer engedélyezett és aktív, a felhasználói linger engedélyezve van. Az első automatikus futás a bekapcsoláskor 2026-11-25 03:17:59 CET-re volt ütemezve. Az automatikus futás eredménye ekkor még ellenőrzésre várt.

## 0. Mit csinál és milyen korlátai vannak?

- Egy futás eredménye **pontosan egy** új, időbélyegzett `.tar.age` fájl, amely titkosítva tartalmazza a MySQL-dumpot, minden fotót, explicit felsorolt helyi konfigurációkat, metaadatokat és SHA-256 ellenőrzőösszegeket.
- **Nem** írja felül a régi archívumot, a sikeres mentés után 14 napos alapretenciót alkalmaz, legalább 2 friss mentést megtart.
- A konzisztens pillanatképhez az **Inventory szolgáltatást a teljes SQL-dump és fotómásolás idejére leállítja**, majd azonnal újraindítja. A Cloudflare oldalon ezen idő alatt átmeneti alkalmazáskiesés lesz. Az Imaging szolgáltatáshoz és a MySQL-hez nem nyúl. **Egyéb, közvetlen DB-/fotóíró esetén a közös pillanatkép nem garantált.**
- Szükséges tárhely: az eredeti adatok mellett a teljes ideiglenes SQL-dump és fotómásolat, plusz az új végleges titkosított archívum és retenciós fájlok. Ezt előre ellenőrizni kell.
- Ez **csak helyi mentés**; a gép vagy meghajtó elvesztése esetén a mentés is elveszhet. Később külső, titkosított másodpéldány szükséges.
- A szerveren a mentés idején az ideiglenes fájlok **titkosítatlanok, de 700/600 jogosultságú mappában vannak**. A kész archívum `age` titkosítást használ; a titkos kulcsot **külön helyen, biztonságosan őrizd meg**.

## 1. Belépés és az aktuális állapot ellenőrzése

Az Ubuntu szerverre a már használt Tailscale + SSH kapcsolattal lépj be az `apollo` felhasználóként (iSH vagy terminál). Az alábbi repóútvonal dokumentált kiindulópont, **előbb ellenőrizd, valóban ezen a helyen van-e a kód**:

```bash
cd /home/apollo/otthontar-ubuntu-teszt/borosCode
pwd
git status --short
git branch --show-current
systemctl --user status otthontar-inventory.service --no-pager
systemctl --user status otthontar-imaging.service --no-pager
```

Ha a munkafában nem commitolt változás van, azt őrizd meg. **A mentés telepítéséhez NEM váltunk `dev` ágra**, hogy ne módosítsuk a működő `main` alkalmazás munkapéldányát. Kizárólag a szükséges szkriptet és ellenőrzőt másoljuk át egy külön, felhasználói programkönyvtárba a GitHubról:

```bash
git fetch origin dev
install -d -m 700 "$HOME/.local/lib/otthontar"
git show origin/dev:scripts/Backup-Ubuntu.sh > "$HOME/.local/lib/otthontar/Backup-Ubuntu.sh"
git show origin/dev:scripts/Verify-Backup-Ubuntu.sh > "$HOME/.local/lib/otthontar/Verify-Backup-Ubuntu.sh"
chmod 700 "$HOME/.local/lib/otthontar/"*.sh
```

Ezek a parancsok **nem váltanak Git-ágat, nem telepítik újra az Otthontárat**, és nem indítják újra a szolgáltatásokat. Ha a `git fetch` vagy `git show` hibázik, ne folytasd a mentés aktiválását.

Ellenőrizd a **valós** DB/fotóbeállításokat a futó systemd egységből vagy az engedélyezett lokális konfigurációból. Titkokat ne másolj képernyőképre, chatbe vagy GitHubra:

```bash
systemctl --user cat otthontar-inventory.service
command -v mysql mysqldump age
df -h
du -sh /A/TENYLEGES/PHOTO_DIR
```

Ha a `systemctl cat` titkot jelenít meg, ne oszd meg a kimenetet. A `PHOTO_DIR`, MySQL host/port, adatbázisnév és hitelesített mentőfelhasználó értékét innen, **nem találgatásból** kell megállapítani.

## 2. Telepítsd a szükséges csomagokat

```bash
sudo apt update
sudo apt install -y age default-mysql-client tar coreutils util-linux
```

MySQL kompatibilis kliens legyen a tényleges szerverhez. Ellenőrizd:

```bash
age --version
mysqldump --version
mysql --version
```

A szkript a rendelkezésre álló kliens alapján csak akkor kapcsolja be a `--set-gtid-purged=OFF` és `--no-tablespaces` paramétereket, ha a `mysqldump --help` szerint elérhetők; így MySQL és MariaDB-klienssel is indítható a megfelelő jogosultságok ellenőrzése után. A valódi dump/restore kompatibilitást ettől még külön próbáld ki.

## 3. Age titkosítókulcs – fontos!

**Ajánlott:** a privát kulcsot másik, biztonságos eszközön állítsd elő és őrizd meg, és **csak a nyilvános címzettet (recipient)** add át az Ubuntu konfigurációnak. A szervernek a mentéshez nincs szüksége a privát kulcsra.

Példa egy megfelelően védett másik Linux/macOS gépen:

```bash
umask 077
age-keygen -o otthontar-backup-identity.txt
age-keygen -y otthontar-backup-identity.txt
```

A második parancs egy `age1...` kezdetű **nyilvános címzettet** ad vissza. A privát identitásfájlt biztonságosan, a szervertől elkülönítve tárold (és legyen további védett másolat). A privát kulcs elvesztése esetén az archívum nem fejthető vissza.

## 4. Az Ubuntu konfigurációja

```bash
mkdir -p -m 700 ~/.config/otthontar
mkdir -p -m 700 ~/otthontar-backups
chmod 700 ~/.config/otthontar ~/otthontar-backups
```

Hozz létre egy **csak saját felhasználó által olvasható** MySQL opciófájlt `~/.config/otthontar/mysql-backup.cnf` néven:

```ini
[client]
host=127.0.0.1
port=AZ_ELLENORZOTT_MYSQL_PORT
user=AZ_ELLENORZOTT_MENTESI_DB_FELHASZNALO
password=AZ_ELLENORZOTT_JELSZO
protocol=tcp
```

A MySQL-felhasználónak a szükséges mentési jogokat kell megadni (adatolvasás, VIEW/TRIGGER/EVENT/ROUTINE esetén a szükséges olvasási jogok), ne root-jelszót használj alapértelmezésként. A fájl létrehozásakor:

```bash
chmod 600 ~/.config/otthontar/mysql-backup.cnf
```

Hozd létre `~/.config/otthontar/backup.conf` néven:

```bash
# Ezt a fájlt az alábbi ellenőrzött értékekkel töltsd ki.
BACKUP_DIR="$HOME/otthontar-backups"
PHOTO_DIR="/A/TENYLEGES/PHOTO_DIR"
MYSQL_DEFAULTS_FILE="$HOME/.config/otthontar/mysql-backup.cnf"
MYSQL_DATABASE="inventory"
AGE_RECIPIENT="age1IDE_A_SAJAT_NYILVANOS_CIMZETTED"
INVENTORY_SERVICE="otthontar-inventory.service"
RETAIN_DAYS=14
# Ugyanaz a zárolás, amit az Update-Ubuntu.sh használ: ne versenyezzenek.
UPDATE_LOCK="$HOME/otthontar-ubuntu-teszt/borosCode/.local/ubuntu/update.lock"
# A szükséges helyi konfigurációk (például a futó Inventory konfigurációja):
CONFIG_FILES=(
  "$HOME/.config/otthontar/mysql-backup.cnf"
  # "/TENYLEGES/UTVONAL/otthontar.env"
)
```

A `backup.conf` szándékosan **megbízható Bash-konfigurációként** van betöltve, ezért ne engedd más felhasználónak módosítani:

```bash
chmod 600 ~/.config/otthontar/backup.conf
```

Az `UPDATE_LOCK` csak akkor legyen beállítva, ha a helyi `Update-Ubuntu.sh` valóban ezt a zárfájlt használja, és a szülőmappa létezik és írható. Ezzel a mentés és az alkalmazásfrissítés nem futhat egyszerre. A `PHOTO_DIR` és a helyi konfigurációk pontos útvonalát kötelezően ellenőrizd. A `CONFIG_FILES` lista az archívumba titkosítva kerül; az archívumot és a kulcsot ennek megfelelően védd. Az `AGE_RECIPIENT` mezőbe **ne** a privát kulcsot írd!

## 5. Kézi, biztonságos előellenőrzés

```bash
bash "$HOME/.local/lib/otthontar/Backup-Ubuntu.sh" --check
```

Ez **nem állítja le az alkalmazást**, nem hoz létre mentést. Ellenőrzi a jogosultságokat, az eszközöket, a DB-kapcsolatot és az Inventory szolgáltatás állapotát. Hibát előbb javítsd ki, csak azután indíts éles mentést.

## 6. Az első mentés (rövid kieséssel)

Olyan időpontban indítsd, amikor az Otthontárat senki nem használja, mert a mentés alatt a leltár nem lesz elérhető:

```bash
bash "$HOME/.local/lib/otthontar/Backup-Ubuntu.sh" --run
ls -lh ~/otthontar-backups/otthontar-*.tar.age
systemctl --user is-active otthontar-inventory.service
```

Egy sikeres futás egy új `otthontar-YYYYMMDDTHHMMSSZ-PID.tar.age` fájlt ad. **Még ne állítsd be az automatikus futást, amíg az első mentés és a visszafejthetőség nincs ellenőrizve.**

## 7. Mentés ellenőrzése és próbavisszaállítás

A legbiztonságosabb, ha az archívumot átmásolod egy külön, ellenőrzött gépre, ahol a privát `age` identitást tartod. Az ellenőrzés nem módosítja az éles adatokat:

```bash
bash "$HOME/.local/lib/otthontar/Verify-Backup-Ubuntu.sh" "/A/MENTES/FAJL.tar.age" "/A/PRIVAT/AGE/IDENTITY"
```

Az ellenőrzés feloldja az archívumot **ideiglenes, privát könyvtárba**, és ellenőrzi a SHA-256 állománylistát. Ehhez a privát kulcsot **ne másold tartósan az Ubuntu szerverre**.

**Ez még nem teljes visszaállítási teszt.** A visszafejtett `database.sql` fájlt külön teszt-MySQL adatbázisra/szerverre importáld; ellenőrizd a sor- és fotószámokat, az adatbázisbeli fotóreferenciákat, valamint hogy a fotók olvashatók. A mentés `--databases` dumpformátuma `CREATE DATABASE/USE` utasításokat tartalmazhat, ezért ne importáld vakon éles MySQL szerverre vagy nem izolált helyre! Készíts ehhez külön, teljesen elkülönített teszt-MySQL környezetet, és előbb nézd meg a SQL fájlt. Ne írj felül éles adatot.

## 8. Napi automatikus időzítés

Miután a kézi mentés és az ellenőrzés sikerült:

```bash
mkdir -p ~/.config/systemd/user
cd /home/apollo/otthontar-ubuntu-teszt/borosCode
git show origin/dev:scripts/systemd/otthontar-backup.service > ~/.config/systemd/user/otthontar-backup.service
git show origin/dev:scripts/systemd/otthontar-backup.timer > ~/.config/systemd/user/otthontar-backup.timer
```

A service a külön telepített `%h/.local/lib/otthontar/Backup-Ubuntu.sh` példányt indítja, **nem** a futó alkalmazás repóját. Ellenőrizd, hogy az új fájl valóban megvan: `systemctl --user cat otthontar-backup.service` és `ls -l ~/.local/lib/otthontar/Backup-Ubuntu.sh`.

```bash
systemctl --user daemon-reload
systemctl --user enable --now otthontar-backup.timer
systemctl --user list-timers --all | grep otthontar-backup
systemctl --user status otthontar-backup.timer --no-pager
```

Az időzítő **naponta 03:15-kor**, plusz legfeljebb 15 perc random késleltetéssel fut a gép saját időzónájában; `Persistent=true` az elmaradt indítás pótlására szolgál. A felhasználói systemd csak akkor képes bejelentkezés nélkül/újraindítás után futni, ha a felhasználói service manager megfelelően működik, például a felhasználónál engedélyezett `linger`-rel. A tényleges beállítást `loginctl show-user "$USER" -p Linger` paranccsal ellenőrizd. `sudo loginctl enable-linger "$USER"` csak akkor szükséges, ha ezt kifejezetten akarod és ellenőrizted a felhasználói rendszerindítás feltételeit.

Naplók:

```bash
journalctl --user -u otthontar-backup.service -n 100 --no-pager
systemctl --user list-timers --all | grep otthontar
```

A naplóban nem szabad DB-jelszónak, titkosító kulcsnak vagy személyes tárgyadatnak szerepelnie.

## 9. Hibaelhárítás, leállítás és biztonság

- **Az időzítőt leállítani:** `systemctl --user disable --now otthontar-backup.timer`. Ez nem töröl mentést vagy alkalmazásadatot.
- **Hiányzó age:** ellenőrizd a telepítést és a `PATH`-ot; a szkript hiba esetén nem publikál végleges archívumot.
- **MySQL hibás hozzáférés:** ellenőrizd a mentési jogosultságokat és a valós adatbáziscímet/jelszót, ne tegyél jelszót parancssorba.
- **Inventory szolgáltatás leállva:** a mentőszkript ilyen esetben nem áll neki, és nem próbál találomra újraindítani; külön vizsgáld a szolgáltatást.
- **Tárhely:** a teljes fotómappa másolata és az archívum egyszerre lehet a lemezen, előre mérd fel a szabad kapacitást; retenció csak sikeres mentés után fut.
- **Közvetlen DB-/fotóíró:** a szkript csak az Inventory rendszerírásait állítja le. Ha külső író is van, az archivált DB és fotók időpillanata eltérhet.
- **Teljes gépvesztés:** a helyben maradó mentés nem megoldás; később külső titkosított másodpéldány és tényleges restore-gyakorlat kell.

## 10. Kódhoz kötött tesztek

A fejlesztői repóban:

```bash
bash -n scripts/Backup-Ubuntu.sh
bash -n scripts/Verify-Backup-Ubuntu.sh
bash -n scripts/tests/Backup-Ubuntu.Tests.sh
bash scripts/tests/Backup-Ubuntu.Tests.sh
```

Az izolált fixture-tesztek **mockolt** MySQL/systemd/age eszközökkel futnak; nem bizonyítják a tényleges age-titkosítást, élő MySQL-fotó konzisztenciát vagy a rendszer saját timerét. A mentés akkor tekinthető üzemkésznek, ha a valós Ubuntu előellenőrzés, titkosított archívum, ellenőrzött próbavisszaállítás és automatikus időzítő is igazolt.

**Későbbi feladat:** a helyi, titkosított archívum külső/független tárhelyre másolása és onnan történő rendszeres visszaállítási próba.
