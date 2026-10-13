## Context

`:domain` computes each product's `NextAlert` (`Scheduled(at)`, `Due`, `None`) from an explicit moment and time zone (`depletion-and-alert`), independent of delivery. `:app` has Room schema 1 (`products`), a manual `AppContainer` (ADR 0015) exposing `clock` and `zone`, and no background components. Spike #38 (issue comment) and its owner decisions fix the mechanism: inexact alarms at 09:00 local (the mechanism was refined after the experiment, see Decision 9), idempotent reconcile on boot, update, time zone, clock and app start, missed alerts delivered late until the depletion date, force-stop risk accepted.

## Goals / Non-Goals

**Goals:**
- Exactly one notification per product alert, never lost while the product is not yet depleted, never repeated.
- One reconcile path used by every trigger, so there is a single place to reason about correctness.
- Domain rules (what to do for a product) in `:domain`; Android mechanics in adapters.

**Non-Goals:**
- Permission request, rationale and disabled-alerts banner (`add-notification-permission`), exact alarms, calendar.

## Decisions

1. **Delivery decision in `:domain`.** A pure function, e.g. `AlertAction.decide(nextAlert, delivered)`, returns `Schedule(at)` (and clear the record), `Deliver`, `Nothing` or `Cancel`. An `AlertReconciler` domain service iterates the products from `ProductRepository`, reads and writes delivery records through an `AlertDeliveries` port, and drives two ports: `AlertScheduler` (`schedule(id, at)`, `cancel(id)`) and `AlertNotifier` (`notify(product, depletionDate): Boolean`, false when notifications are disabled). Fully unit-testable with fakes and a fixed clock and zone.
2. **Delivery record: separate table `alert_deliveries(product_id PRIMARY KEY, FOREIGN KEY ... ON DELETE CASCADE)`** rather than a column on `products`: the product row stays a pure product, and the cascade removes the record with the product. Schema 2 with a `Migration(1, 2)` creating the table, an exported `app/schemas/.../2.json`, and a migration test from the exported schema 1. Recording a delivery for a product deleted in the meantime inserts nothing instead of violating the foreign key.
3. **A "re-arm" rule instead of keying deliveries by alert date.** Clearing the record whenever the next alert is `Scheduled` means a delivered alert stays delivered through small corrections (still `Due`) and re-arms only when the alert moves into the future (restock, longer stock). Keying by alert date was rejected: removing one unit would shift the date and alert again.
4. **One `PendingIntent` per product, distinguished by data URI** (`customnotifier://alert/<uuid>`) rather than a hashed request code, so two products never collide; `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`. Canceling uses the same intent.
5. **Canceling deleted products' alarms:** the in-process trigger diffs the previous and current product ids and cancels removed ones; after a process restart an alarm for a missing product simply finds no product and posts nothing (spec), so no alarm registry is needed.
6. **Triggers.** One manifest receiver for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET` (exempt implicit broadcasts) and one non-exported receiver for the alarm itself; both call the reconciler inside `goAsync()` on a background dispatcher, well within the receiver time limit. App start: the application starts an application-scope collector of `ProductRepository.observeAll()` that reconciles on the first emission and every later change, which also covers every create, edit, restock, adjustment and delete without touching the ViewModels.
7. **Notification adapter.** Channel "Depletion alerts", default importance, created in `Application.onCreate`. `notify` returns false without posting when `NotificationManagerCompat.areNotificationsEnabled()` is false or the channel is blocked (no record then, so it is retried). Notification id derived per product (stable mapping stored with the record or `uuid.hashCode()` with the tag set to the uuid, so collisions are impossible), content intent opens `MainActivity` with `FLAG_IMMUTABLE`, auto-cancel. Small icon: a monochrome vector drawable added with this change (the launcher icon is #17).
8. **ADR 0017** records the inexact-alarm decision and the measured delays (experiment, run with the first mechanism, `setAndAllowWhileIdle`): on `cn-api26` and `cn-api37`, schedule an alarm 2 minutes ahead, `adb shell dumpsys deviceidle force-idle`, record delivery; repeat with `adb shell am set-standby-bucket dev.jdgomez.customnotifier restricted`. Results are reported in the ADR (no committed scripts).
9. **Bounded window with `setWindow` (owner decision after the experiment).** Each alert is scheduled with `AlarmManager.setWindow(RTC_WAKEUP, at, 3 hours, pendingIntent)`. The first mechanism, `setAndAllowWhileIdle`, has a heuristic window of 75% of the time between setting the alarm and its trigger; the experiment showed delivery at the end of that window on API 37, so an alert set 20 days ahead could arrive up to 15 days late, after the depletion date. `setWindow` has a window independent of the lead time. The window is 3 hours: the lead time is whole days, so a few hours of delay never matters, a longer window only widens the uncertainty, and the platform minimum for a permission-free inexact window is 10 minutes, so there is ample margin. Rejected: `setAndAllowWhileIdle` (window scales with the lead time); a periodic re-arm (a wake-up per day or hour, against ADR 0007); exact alarms (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`, a Play declaration this app does not meet, spike #38). Known limit: `setWindow` alarms are not allowed while idle, so in deep Doze delivery waits for the next maintenance window or until the device leaves Doze (the user picking it up); the reconcile on app start and every other trigger delivers late. The measurements are in ADR 0017.

## Risks / Trade-offs

- [In deep Doze a window alarm waits for a maintenance window or for the device to be used] -> the alert is a few hours late at most in normal use; documented in ADR 0017 and the spec.
- [Force stop cancels alarms until the next app start] -> accepted (spike #38); reconcile on start delivers late.
- [OEM battery managers may delay or drop alarms] -> late delivery on next start; not testable on emulators; documented in ADR 0017.
- [On API 33+ notifications are off until part b asks for the permission] -> part b follows next; nothing is released before #47.
- [Reconciling all products on every change] -> a handful of products, trivial cost; no polling.

## Owner decisions (UX approved 2026-10-13)

1. #45 is split as drafted: this change includes the channel and posting the notification; part b, `add-notification-permission`, is only the permission request, rationale, denial handling and disabled-alerts banner.
2. Notification texts: title "Buy <name>", text "Runs out <date>" or "Runs out today".
3. One alert per cycle (Decision 3): once delivered, adding or removing a few units does not alert again; only an alert moved into the future re-arms it.
4. Tapping the notification opens the product list. To validate with testers (#50): the user may only want to dismiss it ("read it, I will handle it"), which swiping already does; the tap action may change later.

## Testing

- Domain unit tests: the decision table, the reconciler with fakes (schedule, deliver once, re-arm, cancel, notifications disabled retried, missing product).
- Room migration test 1 -> 2 from the exported schema; DAO tests for the delivery table and cascade.
- Robolectric tests for the adapters: `ShadowAlarmManager` (one alarm per product, window start at the right instant, 3 hour window, cancel), `ShadowNotificationManager` (texts, channel, disabled), receivers calling the reconciler.
- E2E on the emulator (permission granted via the test runner): a due product posts the notification with the specified texts (UiAutomator on the shade), tapping opens the list; a scheduled product has a pending alarm (`dumpsys alarm`); boot simulated with `am broadcast` where possible.
- Merged-manifest check: no exact-alarm permission.
