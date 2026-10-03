#!/usr/bin/env bash
# Runs the instrumented E2E suite on the booted emulator and records the screen.
# Output: build/e2e/e2e-run.webm (video) and build/e2e/reports-* (test reports).
# The exit code is Gradle's. Boot an emulator first (docs/development-setup.md).
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
./gradlew connectedDebugAndroidTest
