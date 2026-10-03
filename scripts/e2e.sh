#!/usr/bin/env bash
# Runs the instrumented E2E suite on the booted emulator and records the screen.
# Output: build/e2e/e2e-run.webm (video) and build/e2e/reports-* (test reports).
# Exits non-zero if Gradle fails or if no instrumented test was executed.
# Boot an emulator first (docs/development-setup.md).
set -uo pipefail

cd "$(dirname "$0")/.."

out=build/e2e
video="$out/e2e-run.webm"
# Upper bound for emulator-level recording, in seconds.
time_limit=1800

# Screen recording uses `adb emu`, so exactly one emulator and no other device.
devices=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
if ! [[ "$devices" =~ ^emulator-[0-9]+$ ]]; then
  echo "Exactly one running emulator required: boot one emulator (see docs/development-setup.md)." >&2
  exit 1
fi

# Gradle installs the test APKs, and on a device that is still booting the install
# fails ("device is still booting") while Gradle still ends green with no tests run.
# Wait for the boot to finish and for the package manager to answer.
boot_timeout=300
echo "Waiting for the emulator to finish booting..."
waited=0
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] \
  && adb shell pm path android >/dev/null 2>&1; do
  if [ "$waited" -ge "$boot_timeout" ]; then
    echo "The emulator did not finish booting within ${boot_timeout}s." >&2
    exit 1
  fi
  sleep 2
  waited=$((waited + 2))
done

rm -rf "$out" app/build/reports/androidTests app/build/outputs/androidTest-results
mkdir -p "$out"

# Runs on every exit path, so the video and reports survive a failing run.
finish() {
  adb emu screenrecord stop >/dev/null 2>&1
  # The emulator finalizes the video file asynchronously.
  sleep 3
  for dir in app/build/reports/androidTests app/build/outputs/androidTest-results; do
    [ -d "$dir" ] && cp -r "$dir" "$out/reports-$(basename "$dir")"
  done
  echo "E2E video: $video"
  echo "E2E reports: $out/reports-*"
}

if ! adb emu screenrecord start --time-limit "$time_limit" "$PWD/$video" >/dev/null; then
  echo "Could not start the emulator screen recording." >&2
  exit 1
fi
trap finish EXIT

./gradlew connectedDebugAndroidTest || exit $?

# Gradle can end green without running anything (failed install, filter matching
# nothing), so count the executed tests in the JUnit XML it wrote.
executed=0
for xml in app/build/outputs/androidTest-results/connected/*/TEST-*.xml; do
  [ -f "$xml" ] || continue
  count=$(grep -o '<testsuite [^>]*' "$xml" | grep -o ' tests="[0-9]*"' | grep -o '[0-9]*' | awk '{n += $1} END {print n + 0}')
  executed=$((executed + ${count:-0}))
done
if [ "$executed" -eq 0 ]; then
  echo "No instrumented tests were executed." >&2
  exit 1
fi
echo "Instrumented tests executed: $executed"
