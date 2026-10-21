## 1. Settings state

- [x] 1.1 Add `ChannelSwitch` and `SettingsState.channel`, set it in `recompute`, and disable the switch while `Loading` (design decisions 1 and 2)
- [x] 1.2 Tests: view model loading state, Compose test of the disabled switch; update existing tests to the new state; screenshot references unchanged (design decision 5)

## 2. End to end

- [x] 2.1 Add `awaitSettingsSwitchEnabled()` and use it in `CalendarBannerTest` and `CalendarChannelTest` before tapping the switch
- [x] 2.2 Run `./gradlew lintAll test` and `scripts/e2e.sh` on one emulator; shut it down afterwards

## 3. Archive

- [x] 3.1 Run `openspec archive settings-switch-waits-for-state`, sync the delta spec into `openspec/specs/calendar-channel`, and update the Project state in `AGENTS.md` in one line
