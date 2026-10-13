# 0017. Alerts use inexact alarms, no exact-alarm permission

Date: 2026-10-13
Status: Proposed

## Context

Alerts are scheduled once with the OS ([0007](0007-deterministic-alert-scheduling.md)). Spike #38 and the owner decided on one inexact `setAndAllowWhileIdle(RTC_WAKEUP)` alarm per product at 09:00 local on the alert date, without `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` (a Play declaration that a consumable tracker does not meet), with reconciling on boot, update, clock and time zone changes, app start and every product change. This change implements it (`add-alert-scheduler`); this ADR records the experiment that measures how late such an alarm arrives.

## Decision

- One alarm per product, `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, at, pendingIntent)`, identified by a per-product data URI.
- No exact-alarm permission and no background polling. A merged-manifest test guards this.
- A missed alert is delivered late, until the depletion date, by the reconcile on boot or app start.

## Experiment

Method (no committed scripts): a product with a 09:00 alert 20 days ahead is stored, the device clock is moved to 08:58 on the alert date (which makes the app reconcile and set the alarm about 2 minutes ahead), the mode below is applied 8 seconds later and the moment the alarm fired is read from `dumpsys alarm` after 4 minutes. The notification was posted every time.

| Emulator | Mode | Alarm fired |
|---|---|---|
| `cn-api26` | normal | 09:00:00 |
| `cn-api26` | `dumpsys deviceidle force-idle` | 09:00:08 |
| `cn-api26` | standby bucket restricted | not applicable (buckets exist from API 28) |
| `cn-api37` | normal | 09:01:30 |
| `cn-api37` | `dumpsys deviceidle force-idle` | 09:01:30 |
| `cn-api37` | `am set-standby-bucket ... restricted` | 09:01:30 |

On API 37 the alarm fired at the end of its inexact window (`maxWhenElapsed`, 75% of the time between setting the alarm and its trigger moment, 84 seconds here), not at the requested moment, in every mode. On API 26 it fired at the requested moment.

## Consequences

- The delay is bounded by the window, which grows with how far ahead the alarm was set: an alarm set 20 days ahead may be delivered up to 15 days late on API 37 when nothing else wakes the device. The experiment could not reproduce this case in real time. With a lead time shorter than that window the alert can arrive after the depletion date, which the product promise ("never run out") does not allow. The decision of whether to keep this mechanism, or bound the window (for example `setWindow` with a short window, or re-arming nearer the date), needs the owner before release; until then this ADR stays Proposed.
- Every reconcile re-sets the alarms, which resets the window relative to that moment; opening the app, a boot or any product change shortens it.
- Force stop and OEM battery managers can still delay or drop alarms until the next app start (accepted in spike #38, not testable on emulators).
- On API 33+ the notification is shown only when the permission is granted; the request is `add-notification-permission`.
