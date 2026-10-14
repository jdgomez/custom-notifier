## 1. Domain

- [x] 1.1 Add `CalendarId`, `DeviceCalendar`, `CalendarChannel` (`Off`, `On`), the `CalendarGateway` port (`writableCalendars`, `find`) and the `CalendarChannelSettings` port (`observe`, `set`) in `:domain` (design decision 1)
- [x] 1.2 Add the glossary term "Device calendar" to `docs/SESSION0.md`: a calendar on the device that can receive alert events

## 2. Adapters

- [x] 2.1 Declare `READ_CALENDAR` and `WRITE_CALENDAR` in the manifest; add `ProviderCalendarGateway` in the new `calendar` package (design decision 2)
- [x] 2.2 Add the `calendar_channel` table, DAO, `RoomCalendarChannelSettings`, database version 3 with `MIGRATION_2_3`, commit `app/schemas/3.json`; Room tests for the adapter and a migration test from version 2 (design decision 3)
- [x] 2.3 Add `CalendarAccess` (`Granted`, `Missing(canAsk)`) with the permanent-denial rule; Robolectric tests for every state (design decision 4)
- [x] 2.4 Wire the gateway, settings and access in `AppContainer`

## 3. Settings screen

- [x] 3.1 Add the `Settings` destination and the gear action in the product list top bar, with the texts and accessibility labels from the spec
- [x] 3.2 Add `SettingsViewModel` and its state (design decision 5), re-checked on `ON_RESUME` and after each permission result; unit tests with fakes for every spec scenario
- [x] 3.3 Add the Settings screen, the "Allow calendar access?", "Calendar access is off", "Choose a calendar" and "No calendar available" dialogs, and the access-removed and calendar-gone messages, with the exact spec texts
- [x] 3.4 Robolectric Compose tests for every spec scenario that needs no system dialog; screenshot references per design decision 6

## 4. End to end

- [x] 4.1 Instrumented tests on `cn-api37` with UiAutomator and test-created local calendars, covering the scenarios in design decision 6
- [x] 4.2 Run `scripts/e2e.sh` on `cn-api37` and check the video; boot `cn-api26`, install and verify by hand that turning the switch on asks for access, the chooser lists the test calendars and the choice survives a relaunch; shut the emulators down
- [ ] 4.3 Check the production line count (about 400 at most, tests excluded); if clearly over, stop and report before going on

## 5. Archive

- [ ] 5.1 Run `openspec archive add-calendar-gateway`, sync the delta spec into `openspec/specs/calendar-channel/spec.md`, and update the Project state in `AGENTS.md` (#46 part a done, next `add-calendar-event-sync`)
