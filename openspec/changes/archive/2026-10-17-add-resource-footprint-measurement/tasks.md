## 1. Shrunk release build

- [x] 1.1 Add the release build type with R8 and resource shrinking, no signing config (design decision 1); `./gradlew assembleDebug assembleRelease lintAll test` passes with zero warnings
- [x] 1.2 Add `assembleRelease` to the `verify` job command in `.github/workflows/ci.yml` (design decision 7)
- [x] 1.3 Hand check of the release APK signed with the debug key (design decision 2) on `cn-api37`, then on `cn-api26` (one emulator at a time): every step of the "Shrunk build behaves like debug" scenario; add a narrow, commented keep rule only for a concrete failure; shut each emulator down afterwards

## 2. Footprint script

- [x] 2.1 Write `scripts/footprint.sh`: device checks, build, local signing, install, seeding (design decisions 2, 3, 5)
- [x] 2.2 Add the measurements: size, RAM (list, form, background alert path), storage, idle background work (design decision 4); check the failure scenarios of the spec (no emulator; a missing value)
- [x] 2.3 Run the script on `cn-api37` and on `cn-api26` (one at a time) and keep the results; if the idle check shows any wakelock, job or unexpected wakeup, stop and report instead of working around it

## 3. Documentation

- [x] 3.1 Write `docs/footprint.md` with the method, how to run it, and both baselines (design decision 6)
- [x] 3.2 Update `AGENTS.md` (Build and run: the release build and the footprint script; CI: the `verify` command) and `docs/development-setup.md` only if a used SDK tool is not covered there
- [x] 3.3 Check the production line count (about 400 at most, tests excluded; the script counts as production); if clearly over, stop and report

## 4. Archive

- [x] 4.1 Run `openspec archive add-resource-footprint-measurement`, sync the delta specs into `openspec/specs/` (`resource-footprint` new, `continuous-integration` modified), and update the Project state in `AGENTS.md` (#49 done; next #47, which needs the owner's Play Developer account)
