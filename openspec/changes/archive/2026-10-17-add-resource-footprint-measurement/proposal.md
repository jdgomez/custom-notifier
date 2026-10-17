## Why

SESSION0 and ADR 0010 require a minimal resource footprint that is measured, not assumed, as a Phase 1 task (#49). Today the release build is not shrunk (no R8, no resource shrinking), so it is neither what the user will install from Play (#47) nor a fair basis for measuring. Decided by the owner on 2026-10-17: shrink the release build in this change, measure that build with a reproducible script, and record a baseline without numeric budgets.

## What Changes

- The release build is shrunk and optimized with R8 (code and resources), with only the keep rules the app really needs. It stays unsigned in Gradle: signing for Play is owner-only (ADR 0014), and the measurement signs its local copy with the debug key outside the build configuration.
- CI's `verify` job also assembles the release build, so a shrinking error fails the pull request.
- New script `scripts/footprint.sh`: on exactly one booted `google_apis` emulator it builds and installs the shrunk release APK, seeds a fixed set of products, and measures:
  - size: APK file size and estimated download size;
  - RAM: PSS of the app process with the product list open, with the product form open, and in the background process woken only by an alert path (app UI never opened);
  - storage: installed app size and app data size with the seeded products;
  - background battery usage: over an idle period with the screen off, the app's alarms, wakeups, wakelocks and jobs, showing that background work is only the one inexact alarm per product with a next alert.
  It writes a machine-readable result and a human summary to `build/footprint/`, also on failure, and exits non-zero when a measurement cannot be taken.
- `docs/footprint.md`: the method, how to run the script, and the baseline results on API 26 (`cn-api26`) and API 37 (`cn-api37`), with the commit and date they were measured on.

No new dependencies: R8 is part of the Android Gradle Plugin; `apkanalyzer`, `apksigner`, `adb` and the emulator's own `sqlite3` are already part of the installed SDK and system images.

## Non-goals

- Numeric budgets or a CI check against them (to be decided at the Phase 1 retrospective, with this baseline).
- Battery drain on a physical device (part of the 8-week tracking, #50).
- Play-delivered split sizes from an Android App Bundle (needs bundletool or the Play Console; measured after #47).
- Running the E2E suite against the shrunk build (the shrunk build is checked by hand in this change).
- Startup time, frame rendering or other performance measurements.
- Release signing configuration (owner-only, #47).

## Capabilities

### New Capabilities
- `resource-footprint`: the shrunk release build, the reproducible footprint measurement (what is measured, on which devices, where results go, when it fails) and the recorded baseline.

### Modified Capabilities
- `continuous-integration`: the `verify` job also assembles the release build.

## Impact

- Depends on: #48 (merged). Not blocked by #47.
- Touches: `app/build.gradle.kts` (release build type), `app/proguard-rules.pro` (new, only if keep rules are needed), `.github/workflows/ci.yml` (`verify` command), `scripts/footprint.sh` (new), `docs/footprint.md` (new), `AGENTS.md` (Build and run, CI, Project state), `docs/development-setup.md` only if a needed SDK tool is not covered there. Uses the emulators: not runnable in parallel with another flow's E2E run.
- Issue: Closes #49.
