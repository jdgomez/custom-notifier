# Resource footprint

ADR [0010](adr/0010-minimal-resource-footprint.md) requires a minimal footprint that is measured, not assumed. This document says what is measured, how to reproduce it, and records the baseline. **No numeric budgets are set yet**; they are decided at the Phase 1 retrospective with this baseline.

## What is measured

The release build is shrunk with R8 (code and resources) and stays unsigned in Gradle (signing for Play is owner-only, ADR 0014). `scripts/footprint.sh` builds it, signs a local copy with the debug key outside the build configuration, installs it on the one running `google_apis` emulator and measures:

| Value | How |
|---|---|
| APK file size, estimated download size | `apkanalyzer apk file-size` and `apkanalyzer apk download-size` on the signed APK |
| RAM with the product list open, with the product form open | total PSS from `dumpsys meminfo`, 5 to 8 seconds after the screen opens |
| RAM in the background process | the app process is killed, then only its alarm receiver is triggered from the root shell (`am broadcast`); no activity starts, which the script checks |
| Installed app size, app data size | `du -sk` of the installed APK directory and of `/data/data/<package>`, after seeding |
| Background work | with the virtual battery unplugged, batterystats reset and the screen off for the idle period: the app's wakelocks, jobs and wakeup alarms (`dumpsys batterystats --checkin`) and its pending alarms (`dumpsys alarm`) |

The app is seeded with a fixed set of 20 products (varied units, rules and lead times; one with its alert date already passed) inserted with `sqlite3` into the app database through `adb root`. The release APK is not debuggable, so `run-as` does not reach it. The inserts name every column, so a schema change makes the script fail instead of measuring a wrong state. Background work must be at most one alarm per product with a next alert and no wakelock, job or other wakeup; otherwise the script fails.

## How to run

Boot one emulator (`docs/development-setup.md`; `cn-api37` or `cn-api26`), no other device, then:

```bash
scripts/footprint.sh
```

The idle period is 10 minutes; `FOOTPRINT_IDLE_SECONDS=60 scripts/footprint.sh` shortens it for a quick try, but record baselines with the default. Results go to `build/footprint/`: `footprint-api<N>.json` (machine-readable), `footprint-api<N>.md` (summary) and `raw/` (command outputs), also on failure. The script exits non-zero, before building, unless exactly one emulator with `adb root` and `sqlite3` is connected and `zipalign`, `apksigner`, `apkanalyzer` and the debug keystore are present, and exits non-zero, naming the value, when one cannot be measured. Shut the emulator down afterwards.

## Baseline

Measured on commit `d2683aa`, 2026-10-17, idle period 600 s, 20 seeded products. Sizes in KB unless noted.

| Value | API 26 (`cn-api26`) | API 37 (`cn-api37`) |
|---|---|---|
| APK file size (bytes) | 1548703 | 1548703 |
| Estimated download size (bytes) | 1343373 | 1343373 |
| RAM (PSS), product list open | 23889 | 32126 |
| RAM (PSS), product form open | 25816 | 25294 |
| RAM (PSS), background process (alert path only) | 12525 | 11138 |
| Installed app size | 4716 | 4084 |
| App data size | 116 | 596 |
| Wakelocks / jobs during idle | 0 / 0 | 0 / 0 |
| Wakeups during idle (all from the alert alarm) | 0 | 0 |
| Pending alarms (20 products) | 19 | 19 |

The 19 alarms are the 20 seeded products minus the one whose alert date had already passed (delivered, so nothing is pending for it). Run-to-run variation of PSS is a few percent.

## Known limits

- Emulator numbers are a baseline to compare builds over time, not absolute values for a phone. The emulator has no real power model, so battery use is reported as counts (wakelocks, jobs, wakeups, alarms), not mAh. Battery drain on a physical device is observed in the 8-week tracking (#50).
- Sizes are for the APK. Play-delivered split sizes from an App Bundle are measured after the first Play upload (#47).
- The script is not run in CI: it needs root and an idle period, and gives emulator-dependent numbers. Re-run it before releases.
