## 1. Banner

- [x] 1.1 Turn `AlertsOffBanner` into a shared banner composable used by both banners, keeping the existing disabled-alerts screenshot references pixel-identical (design decision 1)
- [x] 1.2 Add `CalendarProblem` and its computation in `ProductListViewModel`, re-checked on resume and on channel changes, with the rule shared with `SettingsViewModel` (design decision 2); unit tests for every spec scenario
- [x] 1.3 Show the calendar channel banner with the exact spec texts below the disabled-alerts banner; "Fix" opens Settings (design decision 3)
- [x] 1.4 Robolectric Compose tests and screenshot references per design decision 4

## 2. End to end

- [x] 2.1 Instrumented test on `cn-api37` per design decision 4
- [x] 2.2 Run `scripts/e2e.sh` on `cn-api37` and check the video; boot `cn-api26`, install and verify by hand that revoking calendar access shows the banner and "Fix" opens Settings; shut the emulators down. Run e2e only when no other flow is using an emulator
- [x] 2.3 Check the production line count (about 400 at most, tests excluded); if clearly over, stop and report before going on

## 3. Archive

- [x] 3.1 Run `openspec archive add-calendar-channel-banner`, sync the delta spec into `openspec/specs/calendar-channel-banner/spec.md`, and update the Project state in `AGENTS.md`
