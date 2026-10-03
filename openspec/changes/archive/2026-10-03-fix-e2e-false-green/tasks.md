## 1. Reproduce

- [x] 1.1 Cold-boot `cn-api26` (no snapshot), run `scripts/e2e.sh` as the user does, and confirm the false green: exit 0, no test-result XML; capture the install output and `adb logcat` around the install. Shut the emulator down
- [x] 1.2 Repeat 1.1 on `cn-api37`; record whether it reproduces there
- [x] 1.3 Identify which APK fails to install and why; if it does not reproduce after a few cold boots, record the attempts and continue with group 3

## 2. Root cause

- [x] 2.1 Fix the cause where it lives (no blind retry); if it needs a new dependency or is an upstream bug with no clean fix, stop and escalate with the evidence (owner)
- [x] 2.2 Cold-boot each AVD again and show the first `scripts/e2e.sh` run executes `LaunchTest`

## 3. No-test guard

- [x] 3.1 Force a run that executes zero tests with Gradle green and show `scripts/e2e.sh` exits 0 (before the guard)
- [x] 3.2 Make `scripts/e2e.sh` count executed tests from the result XML and exit non-zero with `No instrumented tests were executed.` when there are none, keeping Gradle's failure status and the video/report evidence
- [x] 3.3 Re-run the forced zero-test case (non-zero now) and a normal run (exit 0, video and report present)

## 4. Docs and gate

- [x] 4.1 Update `AGENTS.md` where it says when `scripts/e2e.sh` exits non-zero
- [x] 4.2 Commit on `change/fix-e2e-false-green` with Conventional Commits; put the reproduction and before/after evidence in the PR
- [x] 4.3 Run the no-mistakes gate with `--skip ci` (no CI on `main` yet)
