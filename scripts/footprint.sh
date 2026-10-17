#!/usr/bin/env bash
# Measures the footprint of the shrunk release APK on the one booted google_apis emulator:
# size, RAM (PSS), storage and background work over an idle period. Method: docs/footprint.md.
# Output: build/footprint/footprint-api<N>.json (machine-readable), footprint-api<N>.md (summary)
# and raw/ (command outputs), also on failure. Exits non-zero when a value cannot be measured.
# Environment: FOOTPRINT_IDLE_SECONDS (idle period with the screen off, default 600).
# Boot an emulator first (docs/development-setup.md).
set -uo pipefail

cd "$(dirname "$0")/.."

pkg=dev.jdgomez.customnotifier
idle_seconds=${FOOTPRINT_IDLE_SECONDS:-600}
out=build/footprint
raw=$out/raw
values=$out/values.tsv

die() {
  echo "$*" >&2
  exit 1
}

# Device checks run before anything is built.
setup_help="The footprint script needs exactly one booted google_apis emulator and no other device, with adb root and sqlite3 (see docs/development-setup.md)."
devices=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
[[ "$devices" =~ ^emulator-[0-9]+$ ]] || die "Exactly one running emulator required. $setup_help"
boot_timeout=300
waited=0
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && adb shell pm path android >/dev/null 2>&1; do
  [ "$waited" -ge "$boot_timeout" ] && die "The emulator did not finish booting within ${boot_timeout}s."
  sleep 2
  waited=$((waited + 2))
done
adb root >/dev/null 2>&1
adb wait-for-device
[ "$(adb shell id -u | tr -d '\r')" = 0 ] || die "adb root is not available on this emulator. $setup_help"
adb shell 'command -v sqlite3' >/dev/null 2>&1 || die "sqlite3 is not available on this emulator. $setup_help"

# The SDK tools the measurement needs, checked before building anything.
build_tools=$(ls -d "${ANDROID_HOME:?ANDROID_HOME is not set}"/build-tools/* 2>/dev/null | sort -V | tail -1)
for tool in zipalign apksigner; do
  [ -x "$build_tools/$tool" ] || die "Missing SDK tool '$tool' (build-tools): see docs/development-setup.md."
done
command -v apkanalyzer >/dev/null || die "Missing SDK tool 'apkanalyzer' (command-line tools, on PATH): see docs/development-setup.md."
[ -f "$HOME/.android/debug.keystore" ] || die "Missing the debug keystore ~/.android/debug.keystore (created by any debug build)."

api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
commit=$(git rev-parse --short HEAD)$(git diff --quiet HEAD && echo "" || echo "-dirty")
date=$(date -u +%Y-%m-%dT%H:%M:%SZ)
json=$out/footprint-api$api.json
summary=$out/footprint-api$api.md

rm -rf "$out"
mkdir -p "$raw"
: >"$values"

record() { printf '%s\t%s\n' "$1" "$2" >>"$values"; }

# Records a numeric value; a missing or non-numeric one fails the run, naming the value.
need() {
  local name=$1 value=$2
  [[ "$value" =~ ^[0-9]+$ ]] || die "Could not measure $name (got '${value}'); see $raw."
  record "$name" "$value"
}

# Runs on every exit path after the checks: restores the device and writes whatever was measured.
finish() {
  local status=$?
  adb shell dumpsys battery reset >/dev/null 2>&1
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
  {
    printf '{\n  "api": %s,\n  "commit": "%s",\n  "date": "%s"' "$api" "$commit" "$date"
    while IFS=$'\t' read -r k v; do printf ',\n  "%s": %s' "$k" "$v"; done <"$values"
    printf '\n}\n'
  } >"$json"
  {
    printf '# Footprint, API %s\n\nCommit %s, measured %s.\n\n| Value | Result |\n|---|---|\n' "$api" "$commit" "$date"
    while IFS=$'\t' read -r k v; do printf '| %s | %s |\n' "$k" "$v"; done <"$values"
  } >"$summary"
  echo "Footprint results: $json, $summary"
  [ "$status" -eq 0 ] || echo "Footprint measurement failed (partial results kept)." >&2
}
trap finish EXIT

# --- Build, sign locally with the debug key (outside the build configuration) and install ---
./gradlew assembleRelease || die "assembleRelease failed."
unsigned=app/build/outputs/apk/release/app-release-unsigned.apk
apk=$out/app-release-debugsigned.apk
"$build_tools/zipalign" -f -p 4 "$unsigned" "$out/aligned.apk" || die "zipalign failed."
"$build_tools/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
  --out "$apk" "$out/aligned.apk" || die "apksigner failed."
rm -f "$out/aligned.apk" "$apk.idsig"

adb uninstall "$pkg" >/dev/null 2>&1
adb install "$apk" >/dev/null || die "Installing the release APK failed."
# The alert path posts notifications; grant the permission where it is a runtime one.
if [ "$api" -ge 33 ]; then
  adb shell pm grant "$pkg" android.permission.POST_NOTIFICATIONS || die "Granting POST_NOTIFICATIONS failed."
fi

# --- Size ---
need apk_file_size_bytes "$(apkanalyzer apk file-size "$apk" 2>"$raw/apk-file-size.err")"
need apk_download_size_bytes "$(apkanalyzer apk download-size "$apk" 2>"$raw/apk-download-size.err")"

# --- Seed 20 products through the root shell ---
# The app creates and migrates the database on its first start; explicit columns make a
# schema change fail this script loudly instead of measuring a wrong state.
adb shell am start -W -n "$pkg/.MainActivity" >/dev/null
db=/data/data/$pkg/databases/custom-notifier.db
waited=0
until adb shell "sqlite3 $db 'SELECT count(*) FROM products'" >/dev/null 2>&1; do
  [ "$waited" -ge 30 ] && die "The app database was not created."
  sleep 1
  waited=$((waited + 1))
done
adb shell am force-stop "$pkg"

# name|unit|package size|units|per days|alert lead days|units in stock now
products='Pills|pills|30|1|1|7|20
Diapers|diapers|40|6|1|3|12
Shampoo|ml|400|20|1|7|300
Coffee|g|500|25|1|5|150
Toilet paper|rolls|12|1|3|7|9
Dish soap|ml|750|15|1|5|300
Vitamins|capsules|60|2|1|10|40
Cat litter|kg|10|1|7|4|3
Toothpaste|tubes|3|1|30|14|2
Contact lenses|lenses|30|2|1|5|14
Laundry detergent|loads|40|1|2|7|30
Razor blades|blades|4|1|14|7|3
Batteries|batteries|8|1|10|10|5
Dog food|kg|15|1|1|5|8
Water filters|filters|6|1|30|15|2
Sponges|sponges|6|1|10|3|4
Trash bags|bags|30|1|1|7|25
Tea|bags|100|3|1|7|90
Salt|g|1000|10|1|14|200
Milk|l|1|1|1|0|3'
now=$(adb shell date +%s | tr -d '\r')
sql=$raw/seed.sql
: >"$sql"
n=0
while IFS='|' read -r name unit package units days lead stock; do
  n=$((n + 1))
  printf "INSERT INTO products (id, name, unit_label, package_size, rate_units, rate_days, stock_numerator, stock_denominator, recorded_at_epoch_second, recorded_at_nano, lead_time_days) VALUES ('00000000-0000-4000-8000-%012d', '%s', '%s', %s, %s, %s, '%s', '1', %s, 0, %s);\n" \
    "$n" "$name" "$unit" "$package" "$units" "$days" "$stock" "$now" "$lead" >>"$sql"
done <<<"$products"
adb push "$sql" /data/local/tmp/seed.sql >/dev/null
adb shell "sqlite3 $db < /data/local/tmp/seed.sql" || die "Seeding the products failed (did the schema change?)."
# Root-written files must stay readable by the app.
uid=$(adb shell stat -c %u "/data/data/$pkg" | tr -d '\r')
adb shell "chown -R $uid:$uid /data/data/$pkg/databases && restorecon -R /data/data/$pkg/databases"
adb shell rm /data/local/tmp/seed.sql
[ "$(adb shell "sqlite3 $db 'SELECT count(*) FROM products'" | tr -d '\r')" = "$n" ] || die "The seeded products are not in the database."
record products_seeded "$n"

# --- PSS in KB: the "TOTAL PSS:" line (API 29+) or the first TOTAL row of the table (older) ---
pss() {
  adb shell dumpsys meminfo "$pkg" | tee "$raw/meminfo-$1.txt" |
    awk '/TOTAL PSS:/ {print $3; exit} /^ *TOTAL +[0-9]/ {print $2; exit}'
}

# App UI with the product list open (the start also schedules the alarms of the seeded products).
adb shell am start -W -n "$pkg/.MainActivity" >/dev/null
sleep 8
need pss_list_kb "$(pss list)"

# Product form open. The add button exposes no text, description or id to the accessibility tree, so it
# is the lowest button of the app window that does not start at the screen edge (list rows are
# full-width buttons, the Settings button is at the top). Tapping a wrong node
# cannot pass: the form must then show its title, read from the app's own string resources.
lang=$(adb shell getprop persist.sys.locale | tr -d '\r' | cut -c1-2)
[ -n "$lang" ] || lang=$(adb shell getprop ro.product.locale | tr -d '\r' | cut -c1-2)
form_label=$(for config in default "$lang"; do
  apkanalyzer resources value --type string --config "$config" --name product_form_title "$apk" 2>/dev/null
done)
[ -n "$form_label" ] || die "Could not read the form title from the APK resources."
# Prints the center of the app node chosen by mode: "button" (the lowest button) or "text" (a node with one of the given labels).
find_node() {
  adb shell uiautomator dump /data/local/tmp/ui.xml >/dev/null 2>&1
  adb shell cat /data/local/tmp/ui.xml | python3 -I -c '
import re, sys
mode, labels, pkg = sys.argv[1], set(sys.argv[2].splitlines()), sys.argv[3]
best = None
for node in re.findall(r"<node [^>]*>", sys.stdin.read()):
    if f"package=\"{pkg}\"" not in node:
        continue
    l, t, r, b = map(int, re.findall(r"\d+", re.search(r"bounds=\"([^\"]*)\"", node).group(1)))
    hit = (mode == "button" and l > 0 and "class=\"android.widget.Button\"" in node) or \
          (mode == "text" and re.search(r" text=\"([^\"]*)\"", node).group(1) in labels)
    if hit and (best is None or t > best[0]):
        best = (t, (l + r) // 2, (t + b) // 2)
if best:
    print(best[1], best[2])
' "$1" "$2" "$pkg"
}
for _ in 1 2 3; do
  fab=$(find_node button)
  [ -n "$fab" ] && break
  sleep 2
done
[ -n "$fab" ] || die "Could not find the add-product button on the product list."
adb shell input tap $fab
waited=0
until [ -n "$(find_node text "$form_label")" ]; do
  [ "$waited" -ge 15 ] && die "The product form did not open; pss_form_kb would be wrong."
  sleep 1
  waited=$((waited + 1))
done
sleep 3
need pss_form_kb "$(pss form)"

# --- Storage in KB, with the seeded products ---
apk_dir=$(dirname "$(adb shell pm path "$pkg" | head -1 | tr -d '\r' | sed 's/^package://')")
need installed_app_size_kb "$(adb shell du -sk "$apk_dir" | tee "$raw/du-apk.txt" | awk '{print $1}')"
need app_data_size_kb "$(adb shell du -sk "/data/data/$pkg" | tee "$raw/du-data.txt" | awk '{print $1}')"

# --- Background process woken only by the alert path (no activity is started) ---
adb shell input keyevent KEYCODE_HOME
sleep 2
adb shell "kill \$(pidof $pkg)"
sleep 2
adb shell pidof "$pkg" >/dev/null && die "The app process did not stop before the background measurement."
timeout 60 adb shell am broadcast --receiver-foreground -n "$pkg/.alerts.AlertAlarmReceiver" >"$raw/alert-broadcast.txt" 2>&1
grep -q "Broadcast completed" "$raw/alert-broadcast.txt" || die "The alert broadcast did not complete; see $raw/alert-broadcast.txt."
sleep 5
adb shell pidof "$pkg" >/dev/null || die "Could not measure pss_background_kb: the alert path did not start the app process."
adb shell dumpsys activity activities | grep -E 'ResumedActivity' >"$raw/resumed-activity.txt"
[ -s "$raw/resumed-activity.txt" ] || die "Could not read the resumed activity (dumpsys format changed?); cannot confirm the app UI stayed closed."
grep -q "$pkg" "$raw/resumed-activity.txt" && die "The alert path opened the app UI; pss_background_kb would be wrong."
need pss_background_kb "$(pss background)"

# --- Idle period with the screen off: the app's alarms, wakeups, wakelocks and jobs ---
adb shell dumpsys battery unplug
adb shell dumpsys batterystats --reset >/dev/null
adb shell input keyevent KEYCODE_SLEEP
echo "Idle for ${idle_seconds}s with the screen off..."
sleep "$idle_seconds"
adb shell dumpsys batterystats --checkin >"$raw/batterystats-checkin-all.txt"
# Positive controls: the dump has the expected format, and the app's own entry was read
# (otherwise zero wakelocks, jobs and wakeups would mean nothing).
grep -q '^9,0,i,vers,' "$raw/batterystats-checkin-all.txt" || die "Unexpected batterystats format (no version record); see $raw."
grep -E "^9,$uid,l," "$raw/batterystats-checkin-all.txt" >"$raw/batterystats-checkin.txt"
grep -q ',l,pr,' "$raw/batterystats-checkin.txt" || die "No batterystats entry for the app (uid $uid); cannot tell idle work from missing data; see $raw."
adb shell dumpsys alarm >"$raw/alarm.txt"
record idle_seconds "$idle_seconds"
# Checkin records of the app's uid: wl = wakelock, jb = job, wua = wakeup alarm (name, count).
need wakelocks "$(grep -c ',l,wl,' "$raw/batterystats-checkin.txt")"
need jobs "$(grep -c ',l,jb,' "$raw/batterystats-checkin.txt")"
need wakeups "$(awk -F, '$4 == "wua" {n += $NF} END {print n + 0}' "$raw/batterystats-checkin.txt")"
need wakeups_not_alert_alarm "$(awk -F, '$4 == "wua" && $5 !~ /AlertAlarmReceiver/ {n += $NF} END {print n + 0}' "$raw/batterystats-checkin.txt")"
need alarms_pending "$(grep -cE "Alarm\{[0-9a-f]+ type [0-9]+ .* $pkg\}" "$raw/alarm.txt")"
# Positive control: the seeded products have next alerts, so no alarm at all means the dump was not understood.
[ "$(awk -F'\t' '$1 == "alarms_pending" {print $2}' "$values")" -gt 0 ] || die "No pending alarm found for the seeded products (dumpsys alarm format changed?); see $raw."

# The background work must be only the alert alarms: at most one per seeded product, nothing else.
violations=$(awk -F'\t' '
  $1 == "wakelocks" || $1 == "jobs" || $1 == "wakeups_not_alert_alarm" {if ($2 > 0) print $1 "=" $2}
  $1 == "alarms_pending" {alarms = $2}
  $1 == "products_seeded" {products = $2}
  END {if (alarms > products) print "alarms_pending=" alarms " (more than " products " products)"}' "$values")
[ -z "$violations" ] || die "Unexpected background work while idle: $violations"
