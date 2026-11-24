#!/usr/bin/env bash
set -euo pipefail
umask 077

branch=${1:-dev}
if [[ $# -gt 1 || ( "$branch" != dev && "$branch" != teszt ) ]]; then
  printf 'Használat: %s [dev|teszt]\n' "$0" >&2
  exit 2
fi

repo=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
profile="$repo/.local/ubuntu"
inventory_jar="$repo/inventory/target/inventory-1.0.0.jar"
imaging_jar="$repo/imaging/target/imaging-1.0.0.jar"
services=(otthontar-inventory.service otthontar-imaging.service)
if [[ ! -x "$profile/jdk/bin/java" || ! -x "$profile/maven/bin/mvn" ||
      ! -s "$inventory_jar" || ! -s "$imaging_jar" ]]; then
  printf 'A meglévő natív Ubuntu-példány JDK-ja, Mavenje vagy JAR-ja hiányzik.\n' >&2
  exit 1
fi

exec 9>"$profile/update.lock"
if ! flock -n 9; then
  printf 'Már fut egy Otthontár-frissítés.\n' >&2
  exit 1
fi

work=$(mktemp -d "$profile/update.XXXXXXXX")
rollback=0

wait_for_health() {
  local attempt
  for ((attempt = 0; attempt < 45; attempt++)); do
    if curl --noproxy '*' -fsS --max-time 2 http://127.0.0.1:18741/actuator/health 2>/dev/null |
         grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' &&
       curl --noproxy '*' -fsS --max-time 2 http://127.0.0.1:18740/otthontar/actuator/health 2>/dev/null |
         grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
      return 0
    fi
    sleep 1
  done
  return 1
}

cleanup() {
  local status=$?
  trap - EXIT
  # Let recovery finish even if the initiating SSH connection drops a second time.
  trap '' HUP INT TERM
  if ((rollback)); then
    printf 'A telepítés megszakadt. Az előző JAR-ok visszaállítása következik.\n' >&2
    if ! { systemctl --user stop "${services[@]}" &&
           cp -p -- "$work/previous-inventory.jar" "$inventory_jar" &&
           cp -p -- "$work/previous-imaging.jar" "$imaging_jar" &&
           systemctl --user start "${services[@]}" &&
           wait_for_health; }; then
      printf 'A visszaállítás ellenőrzése sikertelen. A mentett JAR-ok itt vannak: %s\n' "$work" >&2
      exit 1
    fi
    printf 'Az előző JAR-ok futnak, mindkét helyi állapotvégpont UP.\n' >&2
  fi
  if [[ -e "$work/source/.git" ]]; then
    git -C "$repo" worktree remove "$work/source" || {
      printf 'Az ideiglenes munkafa megmaradt: %s\n' "$work/source" >&2
      exit 1
    }
  fi
  rm -f -- "$work/previous-inventory.jar" "$work/previous-imaging.jar"
  rmdir -- "$work"
  exit "$status"
}

trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

git -C "$repo" fetch --no-tags origin "refs/heads/$branch:refs/remotes/origin/$branch"
commit=$(git -C "$repo" rev-parse --verify "refs/remotes/origin/$branch^{commit}")
git -C "$repo" worktree add --detach "$work/source" "$commit"
(
  cd -- "$work/source"
  env -u DISPLAY JAVA_HOME="$profile/jdk" "$profile/maven/bin/mvn" \
    -B -ntp -Djava.awt.headless=true "-Dmaven.repo.local=$profile/m2" verify
)

new_inventory="$work/source/inventory/target/inventory-1.0.0.jar"
new_imaging="$work/source/imaging/target/imaging-1.0.0.jar"
[[ -s "$new_inventory" && -s "$new_imaging" ]]
cp -p -- "$inventory_jar" "$work/previous-inventory.jar"
cp -p -- "$imaging_jar" "$work/previous-imaging.jar"

rollback=1
systemctl --user stop "${services[@]}"
mv -T -- "$new_inventory" "$inventory_jar"
mv -T -- "$new_imaging" "$imaging_jar"
systemctl --user start "${services[@]}"
wait_for_health
rollback=0
printf 'Frissítés kész: origin/%s @ %s. Mindkét helyi állapotvégpont UP.\n' "$branch" "$commit"
