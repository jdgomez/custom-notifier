## 0. Approval

- [x] 0.1 (owner) Approve the proposal, the #45 split and the UX decisions in design.md (approved 2026-10-13, see design.md Owner decisions)

## 1. Domain

- [x] 1.1 `AlertScheduler`, `AlertNotifier` and `AlertDeliveries` ports, the delivery decision and `AlertReconciler` per design.md
- [x] 1.2 Unit tests for every `alert-delivery` rule that does not need Android

## 2. Persistence

- [x] 2.1 `alert_deliveries` table with cascade, `Migration(1, 2)`, exported schema 2, `AlertDeliveries` Room adapter
- [x] 2.2 Migration test from schema 1 and DAO tests (record, clear, cascade on delete)

## 3. Android adapters and wiring

- [x] 3.1 `AlarmManager` adapter (inexact, idle-allowed, wake-up, per-product data URI)
- [x] 3.2 Notification channel at start and notification adapter (texts as string resources, disabled check, tap opens the list, small icon)
- [x] 3.3 Receivers (boot, package replaced, time zone, time set; alarm) with `goAsync()`, manifest entries and `RECEIVE_BOOT_COMPLETED` / `POST_NOTIFICATIONS`
- [x] 3.4 Application-scope reconcile on start and on every product change, wired in `AppContainer`
- [x] 3.5 Robolectric tests for adapters and receivers; merged-manifest check for exact-alarm permissions

## 4. Evidence and end to end

- [ ] 4.1 E2E tests: due product posts the notification with the exact texts and tapping opens the list; scheduled product has a pending alarm
- [ ] 4.2 Doze and standby experiment on `cn-api26` and `cn-api37` (one emulator at a time); ADR 0017 with the results; ADR index updated
- [ ] 4.3 Run the app on both emulators (light, dark, largest font for the notification), check the notification pixel by pixel, fix anything that looks off
- [ ] 4.4 Update `AGENTS.md` project state; `./gradlew assembleDebug lintAll test` and `scripts/e2e.sh` pass with zero warnings; report the production line count (if it will clearly exceed ~400, stop and report before finishing so the owner can decide on a split)
