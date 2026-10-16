## 1. Domain

- [ ] 1.1 Add `EventId`, `AlertEvent` and the new `CalendarGateway` operations (`canWrite`, `create`, `exists`, `update`, `delete`) in `:domain` (design decision 1); add the glossary term "Alert event" to `docs/SESSION0.md`
- [ ] 1.2 Add `CalendarEventRecord` and the `CalendarEventRecords` port (design decision 2)
- [ ] 1.3 Add `ProductChanges` and the `TrackingProductRepository` decorator; unit tests (design decision 3)
- [ ] 1.4 Add `CalendarEventSync` (design decision 4); unit tests with fakes for every spec scenario in `specs/calendar-events/spec.md`, including idempotency (no gateway writes on a second sync)

## 2. Adapters

- [ ] 2.1 Add the event operations to `ProviderCalendarGateway`: all-day UTC fields, no reminder, existence check before update and delete, `SecurityException` handled (design decision 5); build the texts with the date formatter shared with the notification
- [ ] 2.2 Add the `calendar_event` table, DAO, `RoomCalendarEventRecords`, database version 4 with `MIGRATION_3_4`, commit `app/schemas/4.json`; Room tests for the adapter and a migration test from version 3
- [ ] 2.3 Wire the sync in `AppContainer`: the tracking repository for the ViewModels, the sync after the reconcile on product and channel changes and in `reconcileAlerts`, and `syncCalendar()` called by `SettingsViewModel` after access is granted (design decision 6)

## 3. End to end

- [ ] 3.1 Instrumented tests on `cn-api37` with a test-created local calendar, covering the scenarios in design decision 7
- [ ] 3.2 Run `scripts/e2e.sh` on `cn-api37` and check the video; check by hand in the stock calendar app on `cn-api37` that the event is all-day on the right date with no reminder; boot `cn-api26`, install and verify that turning the channel on creates the events and turning it off deletes them; shut the emulators down. Run e2e only when no other flow is using an emulator
- [ ] 3.3 Check the production line count (about 400 at most, tests excluded); if clearly over, stop and report before going on

## 4. Archive

- [ ] 4.1 Run `openspec archive add-calendar-event-sync`, sync the delta spec into `openspec/specs/calendar-events/spec.md`, and update the Project state in `AGENTS.md` (#46 part b1 done; #46 stays open until `add-calendar-channel-banner`)
