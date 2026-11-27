#!/usr/bin/env bash
# Verify an encrypted snapshot without writing to the live database or photo directory.
set -Eeuo pipefail
umask 077
if [[ $# -ne 2 ]]; then
  echo "Használat: $0 ARCHÍVUM.age AGE_IDENTITY_FILE" >&2; exit 2
fi
archive=$1
identity=$2
[[ -f "$archive" && -f "$identity" ]] || { echo "Hiányzó archívum vagy kulcs." >&2; exit 1; }
for cmd in age tar sha256sum find mktemp; do
  command -v "$cmd" >/dev/null || { echo "Hiányzó program: $cmd" >&2; exit 1; }
done
temp=$(mktemp -d)
trap 'rm -rf -- "$temp"' EXIT
# Identity never appears in command output. Tar extracts only into a private temp directory.
age -d -i "$identity" "$archive" | tar -C "$temp" -xf -
for required in database.sql metadata.txt SHA256SUMS photos config/origins.txt; do
  [[ -e "$temp/$required" ]] || { echo "A mentés hiányos: $required" >&2; exit 1; }
done
[[ -s "$temp/database.sql" ]] || { echo "Az SQL dump üres." >&2; exit 1; }
(
  cd "$temp"
  sha256sum --check --strict SHA256SUMS >/dev/null
)
echo "Sikeres archívum-, tartalom- és SHA-256 ellenőrzés. Az éles adatbázishoz nem nyúltunk."
echo "Ez NEM teljes MySQL visszaállítási próba; a külön tesztadatbázisba történő import még szükséges."
