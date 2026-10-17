## Context

See proposal.md - Why. Current state: `app/build.gradle.kts` has no `buildTypes` block, so `release` uses the AGP defaults (no R8, no resource shrinking, unsigned). The app uses Room 3 (KSP-generated code), Compose, AndroidX Navigation 3, `AlarmManager` receivers and the Calendar Provider. Both local AVDs (`cn-api26`, `cn-api37`) and the CI emulator use `google_apis` system images, which allow `adb root` and ship `sqlite3`. `scripts/e2e.sh` sets the pattern for scripts that need exactly one emulator.

## Goals / Non-Goals

**Goals:**
- A release build that is what the user will run, shrunk, and proven to behave like debug.
- A footprint measurement that anyone (agent or owner) can re-run with one command and get comparable numbers.

**Non-Goals:**
- Making the numbers smaller in this change. If a measurement shows something clearly wrong (for example a wakelock or a job), report it; fixing it is a follow-up change unless it is a one-line configuration fix.

## Decisions

### 1. R8 in the release build type
`release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }`. Keep rules are added only for a concrete failure seen in the hand check, each with a comment naming what needs it; libraries already ship their consumer rules. No `signingConfig` in Gradle (ADR 0014; signing is owner-only).
Alternative: a separate `benchmark` build type initialized from release. Rejected: one more variant to keep in sync, while the measurement only needs the release APK signed locally.

### 2. Local signing outside Gradle
The script copies `app-release-unsigned.apk`, aligns it if needed and signs it with `apksigner` from the installed build-tools using the debug keystore (`~/.android/debug.keystore`). Nothing about signing enters the build configuration, so the release build cannot be shipped debug-signed by mistake.

### 3. Seeding products through the emulator's root shell
The release APK is not debuggable, so `run-as` and instrumentation helpers do not reach its data. The script uses `adb root` (available on `google_apis` images): it launches the app once so Room creates and migrates the database, stops it, inserts a fixed set of 20 products with `sqlite3` into the app's database, then starts the app so the normal reconcile schedules their alarms. The seed covers realistic variety (different rules and units, some products with a next alert soon, one already past its alert date). Inserts use explicit column names, so a schema change makes the script fail loudly instead of measuring a wrong state.
Alternatives: driving the UI with `adb shell input` (fragile coordinates, slow); an instrumented helper (works only on the debug build); a new tool such as Macrobenchmark (new dependency, owner approval, more than this needs).

### 4. How each value is measured
- APK file size and estimated download size: `apkanalyzer apk file-size` and `apkanalyzer apk download-size` on the signed release APK.
- RAM: `dumpsys meminfo <package>` total PSS, read after the screen settles (list open; form open), and in the background process started only by the alert path: with the app process stopped, the script delivers the app's alert path (for example by triggering its pending alarm or sending a system broadcast its receiver listens to, from the root shell) and reads PSS once the receiver has run. The exact trigger is the executor's choice; it must not open any activity.
- Storage: `du` of the installed APK directory and of the app's data directory (root shell), after seeding.
- Background battery usage: unplug the virtual battery (`dumpsys battery unplug`), reset batterystats, turn the screen off, wait an idle period (default 10 minutes, overridable by an environment variable), then read the app's entry from `dumpsys batterystats` (wakelocks, jobs, wakeups) and its pending alarms from `dumpsys alarm`, and restore the battery state. The emulator has no real power model, so the result is stated as counts and durations, not mAh.

### 5. Output and failure
Results go to `build/footprint/`: `footprint-api<N>.json` (one flat object, numeric values with units in the key names, plus `api`, `commit`, `date`) and `footprint-api<N>.md` (summary table). Raw command outputs are kept next to them for inspection. Device checks (exactly one emulator, root available, boot completed, reusing the wait logic style of `scripts/e2e.sh`) run before building. Any value that cannot be read fails the script with its name. A trap restores the battery and screen state and keeps partial results on every exit path.

### 6. Baseline document
`docs/footprint.md`: what is measured and why (ADR 0010), how to run the script, a results table for API 26 and API 37 (commit, date), the idle period used, and the known limits (emulator, no mAh, no Play split sizes). It is linked from `AGENTS.md` (Build and run) and states that budgets are decided later.

### 7. CI
`verify` runs `./gradlew assembleDebug assembleRelease lintAll test`. The footprint script is not run in CI: it needs root, an idle period and gives emulator-dependent numbers; it is a measurement to re-run before releases, not a gate.

## Risks / Trade-offs

- [R8 removes something used only through reflection or from the manifest, breaking a path not covered by JVM tests] → The hand check on both emulators covers every user-facing path in the spec scenario, including alarm delivery, boot/locale receivers and the calendar; any fix is a narrow keep rule with a comment.
- [The CI e2e job still tests the debug build, so a later R8 regression could reach `main` unnoticed] → Accepted for now (non-goal); raise at the Phase 1 retrospective whether E2E should run against the release build before #47 ships.
- [Root-shell seeding couples the script to the database schema] → Explicit columns make it fail loudly on a schema change; the fix is a script update in that change.
- [Emulator numbers differ from a real device] → They are a baseline for comparing builds over time, not absolute values; real-device usage is observed in #50.
- [The idle period makes the script slow] → Default 10 minutes, overridable; documented.
