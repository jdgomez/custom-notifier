## Why

The app shows each product's next alert, but nothing alerts the user: the core promise ("never run out at home, with enough lead time to buy") is not kept yet (issue #45). Spike #38 settled how to schedule alerts on Android; this change implements it.

## What Changes

- An `AlertScheduler` port in `:domain` and an `AlarmManager` adapter in `:app`: one inexact alarm with a bounded delivery window (`setWindow`, `RTC_WAKEUP`, window 3 hours starting at the alert moment) per product at its scheduled next alert, keyed by product identity.
- One idempotent reconcile that rebuilds every product's alarm from the database, run on device boot, app update, time zone change, clock change, app start and every product change (create, edit, restock, adjustment, delete).
- Delivery: when an alert is due and not yet delivered, the app posts one local notification for the product and records the delivery, so a product is alerted exactly once per alert; a missed alert is delivered late on the next boot or app start, until the depletion date.
- A notification channel "Depletion alerts" created at app start, and the notification itself (texts in the spec). Posting is skipped while notifications are disabled; the delivery is then not recorded, so it is retried on the next reconcile.
- Room schema version 2 with a migration, storing the delivery record per product (removed with the product).
- ADR 0017 "Alerts use inexact alarms with a bounded window, no exact-alarm permission", with the Doze and standby delay experiment on `cn-api26` and `cn-api37` as evidence.
- Manifest: `RECEIVE_BOOT_COMPLETED` (normal permission, no prompt) and `POST_NOTIFICATIONS` declared.

No new dependencies.

## Non-goals

- Asking for `POST_NOTIFICATIONS` (API 33+), the rationale, denial handling and the disabled-alerts banner: change `add-notification-permission` (#45 part b). Until then, on API 33+ the notification shows only if the permission was granted in system settings (tests grant it).
- Exact alarms, `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` and their Play declaration (spike #38).
- Calendar events (#46), notification actions (snooze, restock from the notification), custom alert times.
- Working after a force stop until the user opens the app again (accepted risk, spike #38).

## Capabilities

### New Capabilities
- `alert-delivery`: when and how a product's alert is scheduled, delivered once, retried, and shown as a notification.

### Modified Capabilities
- `product-persistence`: the stored data includes each product's alert delivery record; schema version 2 with a migration.

## Impact

- Depends on: #41 (`add-room-persistence`), #40 (`add-depletion-and-alert-domain`), spike #38; all done.
- Touches: `:domain` (scheduler port, delivery decision), `:app` (alarm adapter, broadcast receivers, notification channel and posting, reconcile wiring in `AppContainer` and the application, Room entity, DAO, migration and exported schema, manifest), `docs/adr/`.
- Not parallelizable with changes to persistence or the application start-up; parallelizable with #17 (icon).
