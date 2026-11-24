#!/usr/bin/env bash
# Otthontár: one encrypted archive per completed daily snapshot.
set -Eeuo pipefail
umask 077

usage() {
  echo "Használat: $0 [--check|--run] [konfigurációs-fájl]" >&2
}
mode=${1:---check}
case "$mode" in --check|--run) shift ;; *) usage; exit 2 ;; esac
if (( $# > 1 )); then usage; exit 2; fi
config=${1:-$HOME/.config/otthontar/backup.conf}
if [[ ! -f "$config" || -L "$config" ]]; then
  echo "Hiányzó vagy szimbolikus linket használó konfiguráció: $config" >&2; exit 1
fi
# This file is trusted shell code: restrict owner and permissions before sourcing.
owner=$(stat -c '%u' -- "$config")
perm=$(stat -c '%a' -- "$config")
if [[ "$owner" != "$(id -u)" || $((8#$perm & 8#077)) -ne 0 ]]; then
  echo "A konfiguráció tulajdonosa legyen az aktuális felhasználó, jogosultsága 600." >&2; exit 1
fi
# shellcheck disable=SC1090
source "$config"

: "${BACKUP_DIR:?BACKUP_DIR szükséges}"
: "${PHOTO_DIR:?PHOTO_DIR szükséges}"
: "${MYSQL_DEFAULTS_FILE:?MYSQL_DEFAULTS_FILE szükséges}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE szükséges}"
: "${AGE_RECIPIENT:?AGE_RECIPIENT szükséges}"
INVENTORY_SERVICE=${INVENTORY_SERVICE:-otthontar-inventory.service}
# Explicit optional local configuration files: CONFIG_FILES=(/absolute/path/one /absolute/path/two)
declare -p CONFIG_FILES >/dev/null 2>&1 || CONFIG_FILES=()
[[ "$(declare -p CONFIG_FILES)" == declare\ -a* ]] || { echo "CONFIG_FILES legyen Bash-tömb." >&2; exit 1; }
RETAIN_DAYS=${RETAIN_DAYS:-14}
[[ "$RETAIN_DAYS" =~ ^[0-9]+$ ]] && (( RETAIN_DAYS >= 2 )) || { echo "RETAIN_DAYS >= 2 szükséges" >&2; exit 1; }
[[ "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || { echo "Érvénytelen adatbázisnév" >&2; exit 1; }
[[ "$INVENTORY_SERVICE" =~ ^[a-zA-Z0-9_.@-]+\.service$ ]] || { echo "Érvénytelen szolgáltatásnév" >&2; exit 1; }
for path in "$BACKUP_DIR" "$PHOTO_DIR" "$MYSQL_DEFAULTS_FILE"; do
  [[ "$path" = /* ]] || { echo "Csak abszolút útvonalak engedélyezettek: $path" >&2; exit 1; }
done
[[ -d "$PHOTO_DIR" && ! -L "$PHOTO_DIR" ]] || { echo "A fényképkönyvtár hiányzik vagy symlink." >&2; exit 1; }
if [[ -n "$(find "$PHOTO_DIR" -type l -print -quit)" ]]; then
  echo "A fényképkönyvtár symlinket tartalmaz, kézi ellenőrzés szükséges." >&2; exit 1
fi
[[ -f "$MYSQL_DEFAULTS_FILE" && ! -L "$MYSQL_DEFAULTS_FILE" ]] || { echo "A MySQL-opciófájl hiányzik vagy symlink." >&2; exit 1; }
db_perm=$(stat -c '%a' -- "$MYSQL_DEFAULTS_FILE")
if (( 8#$db_perm & 8#077 )); then echo "MySQL-opciófájl csak a tulajdonos számára legyen olvasható." >&2; exit 1; fi
case "$(realpath -m -- "$BACKUP_DIR")/" in "$(realpath -m -- "$PHOTO_DIR")/"*) echo "A mentési cél nem lehet a fényképkönyvtáron belül." >&2; exit 1 ;; esac
case "$(realpath -m -- "$PHOTO_DIR")/" in "$(realpath -m -- "$BACKUP_DIR")/"*) echo "A fényképek nem lehetnek a mentési cél alatt." >&2; exit 1 ;; esac
for file in "${CONFIG_FILES[@]}"; do
  [[ "$file" = /* && -f "$file" && ! -L "$file" && -r "$file" ]] || { echo "Hiányzó vagy érvénytelen konfigurációs forrás: $file" >&2; exit 1; }
done
for cmd in systemctl mysql mysqldump tar age sha256sum flock cp find sort stat realpath mktemp; do
  command -v "$cmd" >/dev/null || { echo "Hiányzó program: $cmd" >&2; exit 1; }
done
if [[ "$mode" == "--check" ]]; then
  [[ -d "$BACKUP_DIR" ]] || { echo "A mentési könyvtár még hiányzik: $BACKUP_DIR" >&2; exit 1; }
  [[ -w "$BACKUP_DIR" ]] || { echo "A mentési könyvtár nem írható." >&2; exit 1; }
  mysql --defaults-extra-file="$MYSQL_DEFAULTS_FILE" --batch --skip-column-names -e "SELECT 1" "$MYSQL_DATABASE" >/dev/null
  age -r "$AGE_RECIPIENT" </dev/null >/dev/null
  systemctl --user is-active --quiet "$INVENTORY_SERVICE" || { echo "Az Inventory szolgáltatás nem aktív." >&2; exit 1; }
  echo "Előellenőrzés OK: fájlutak, DB-kapcsolat, eszközök, Inventory aktív."; exit 0
fi

[[ -d "$BACKUP_DIR" && ! -L "$BACKUP_DIR" ]] || { echo "A mentési könyvtár hiányzik vagy symlink." >&2; exit 1; }
[[ -w "$BACKUP_DIR" ]] || { echo "A mentési könyvtár nem írható." >&2; exit 1; }
if [[ -n "${UPDATE_LOCK:-}" ]]; then
  [[ "$UPDATE_LOCK" = /* && -d "$(dirname -- "$UPDATE_LOCK")" ]] || { echo "Érvénytelen UPDATE_LOCK útvonal" >&2; exit 1; }
  exec 8>"$UPDATE_LOCK"
  flock -n 8 || { echo "Az alkalmazás frissítése folyamatban; mentés elhalasztva." >&2; exit 1; }
fi
backup_perm=$(stat -c '%a' -- "$BACKUP_DIR")
if (( 8#$backup_perm & 8#077 )); then echo "A mentési könyvtár legyen 700 jogosultságú." >&2; exit 1; fi
exec 9>"$BACKUP_DIR/.backup.lock"
flock -n 9 || { echo "Már fut egy Otthontár-mentés." >&2; exit 1; }
systemctl --user is-active --quiet "$INVENTORY_SERVICE" || { echo "Az Inventory már leállt; nem nyúlok hozzá." >&2; exit 1; }
mysql --defaults-extra-file="$MYSQL_DEFAULTS_FILE" --batch --skip-column-names -e "SELECT 1" "$MYSQL_DATABASE" >/dev/null
age -r "$AGE_RECIPIENT" </dev/null >/dev/null
stage=$(mktemp -d "$BACKUP_DIR/.snapshot.XXXXXXXX")
partial=
stopped=0
cleanup() {
  status=$?
  trap - EXIT
  trap '' INT TERM HUP
  if (( stopped )); then
    if ! systemctl --user start "$INVENTORY_SERVICE" || ! systemctl --user is-active --quiet "$INVENTORY_SERVICE"; then
      echo "KRITIKUS: az Inventory újraindítása sikertelen, kézi beavatkozás szükséges!" >&2
      status=1
    fi
  fi
  [[ -z "$partial" ]] || rm -f -- "$partial"
  [[ -z "$stage" ]] || rm -rf -- "$stage"
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
trap 'exit 129' HUP

mkdir -m 700 "$stage/photos" "$stage/config"
: > "$stage/config/origins.txt"
echo "Inventory írásainak leállítása és konzisztens pillanatkép készítése..."
stopped=1
systemctl --user stop "$INVENTORY_SERVICE"
# InnoDB transaction-consistent SQL dump; all application writes must use Inventory.
dump_opts=(--single-transaction --quick --routines --triggers --databases)
dump_help=$(mysqldump --help 2>&1)
if [[ "$dump_help" == *"--no-tablespaces"* ]]; then dump_opts+=(--no-tablespaces); fi
if [[ "$dump_help" == *"--set-gtid-purged"* ]]; then dump_opts+=(--set-gtid-purged=OFF); fi
mysqldump --defaults-extra-file="$MYSQL_DEFAULTS_FILE" "${dump_opts[@]}" "$MYSQL_DATABASE" >"$stage/database.sql"
[[ -s "$stage/database.sql" ]] || { echo "Üres SQL dump!" >&2; exit 1; }
cp -a -- "$PHOTO_DIR/." "$stage/photos/"
for i in "${!CONFIG_FILES[@]}"; do
  file="${CONFIG_FILES[i]}"
  cp -p -- "$file" "$stage/config/file-$i"
  printf 'file-%s=%s\n' "$i" "$file" >> "$stage/config/origins.txt"
done
if ! systemctl --user start "$INVENTORY_SERVICE" || ! systemctl --user is-active --quiet "$INVENTORY_SERVICE"; then
  echo "KRITIKUS: Inventory újraindítása sikertelen!" >&2; exit 1
fi
stopped=0

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
{
  printf 'timestamp_utc=%s\n' "$timestamp"
  printf 'database=%s\n' "$MYSQL_DATABASE"
  printf 'photo_source=%s\n' "$PHOTO_DIR"
  printf 'host=%s\n' "$(hostname)"
  printf 'git_commit=%s\n' "${APP_COMMIT:-unverified}"
  printf 'consistency=inventory-service-stopped-during-snapshot\n'
  printf 'note=Other writers/direct MySQL users must be quiesced separately.\n'
} > "$stage/metadata.txt"
(
  cd "$stage"
  find database.sql metadata.txt photos config -type f -print0 | sort -z | xargs -0 -r sha256sum > SHA256SUMS
)
name="otthontar-$timestamp-$$.tar.age"
partial="$BACKUP_DIR/.$name.partial"
final="$BACKUP_DIR/$name"
# A complete encrypted file is published atomically only after tar/age succeeded.
tar -C "$stage" -cf - database.sql metadata.txt photos config SHA256SUMS |
  age -r "$AGE_RECIPIENT" -o "$partial"
[[ -s "$partial" ]] || { echo "Üres titkosított mentés!" >&2; exit 1; }
chmod 600 "$partial"
mv -n -- "$partial" "$final"
[[ -e "$partial" ]] && { echo "Azonos nevű mentés már létezik." >&2; exit 1; }
partial=
echo "Mentés létrejött: $final"
# Retain at least the latest two complete archives; prune older files only on success.
mapfile -d '' archives < <(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'otthontar-*.tar.age' -print0 | sort -z)
if (( ${#archives[@]} > 2 )); then
  cutoff=$(date -u -d "$RETAIN_DAYS days ago" +%s)
  for ((i=0; i < ${#archives[@]}-2; i++)); do
    stamp=$(stat -c '%Y' -- "${archives[i]}")
    if (( stamp < cutoff )); then rm -- "${archives[i]}"; fi
  done
fi
