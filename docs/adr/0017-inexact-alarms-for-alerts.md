# 0017. Alerts use inexact alarms with a bounded window, no exact-alarm permission

Date: 2026-10-13
Status: Accepted

## Context

Alerts are scheduled once with the OS ([0007](0007-deterministic-alert-scheduling.md)). Spike #38 decided on inexact alarms at 09:00 local on the alert date, without `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` (a Play declaration a consumable tracker does not meet), reconciled on boot, update, clock and time zone changes, app start and every product change. The first mechanism, `setAndAllowWhileIdle`, was measured in `add-alert-scheduler` and found unsuitable (see Experiment), so the owner chose a bounded window on 2026-10-13.

## Decision

- One alarm per product, `AlarmManager.setWindow(RTC_WAKEUP, at, 3 hours, pendingIntent)`, identified by a per-product data URI: delivered no earlier than the alert moment and, while the device is not in deep idle, at most 3 hours later.
- 3 hours because the lead time is whole days (a few hours never matter), because a window independent of the lead time is what the experiment required, and because API 37 delivers inside the window at a system-chosen moment (4 minutes into a 10 minute window in the check below), so a longer window only widens the uncertainty. The measurements gave no reason to choose another value.
- No exact-alarm permission and no background polling. A merged-manifest test guards this.
- A missed alert is delivered late, until the depletion date, by the reconcile on boot or app start.

## Alternatives considered

- **`setAndAllowWhileIdle` (first mechanism):** its window is heuristic, 75% of the time between setting the alarm and its trigger. Rejected: an alert set 20 days ahead could be delivered up to 15 days late, after the depletion date.
- **Periodic re-arm** (a nearer alarm that arms the real one): bounds the window but wakes the device daily or hourly, against 0007.
- **Exact alarms** (`setExactAndAllowWhileIdle`, `setAlarmClock`): need `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` with a Play declaration this app does not meet (spike #38).

## Experiment

Method (no committed scripts): the moment an alarm fired is read from `dumpsys alarm` (per-package alarm statistics) a few minutes after it was due; deep idle is `dumpsys deviceidle force-idle`.

First mechanism, `setAndAllowWhileIdle`, alert 2 minutes ahead (device clock moved to 08:58 on the alert date, which makes the app set the alarm):

| Emulator | Mode | Alarm due | Fired |
|---|---|---|---|
| `cn-api26` | normal | 09:00:00 | 09:00:00 |
| `cn-api26` | force-idle | 09:00:00 | 09:00:08 |
| `cn-api37` | normal | 09:00:00 | 09:01:30 (end of its 84 s window) |
| `cn-api37` | force-idle | 09:00:00 | 09:01:30 |
| `cn-api37` | standby bucket restricted | 09:00:00 | 09:01:30 |

Chosen mechanism, `setWindow` with 3 hours (no clock change; the alarm is set 2 minutes ahead, and a real alert 20 days ahead is set in the same run):

| Emulator | Mode | Alarm due | Result |
|---|---|---|---|
| `cn-api26` | normal | 15:38:44 | fired 15:39:01 |
| `cn-api26` | force-idle | 15:33:31 | fired 15:33:31 |
| `cn-api37` | normal | 15:44:42 | still pending 3 minutes later, inside its 3 hour window |
| `cn-api37` | force-idle | 15:49:46 | not fired after 5 minutes; deferred by the idle policy (expected) |
| `cn-api37` | normal, 10 minute window check | 15:53:58 | fired 15:58:13, inside the window |
| both | alert 20 days ahead | 2026-11-02 09:00 | `window=+3h0m0s0ms`, `maxWhenElapsed` = trigger + 3 hours (it was +15 days with the first mechanism) |

## Consequences

- The delay no longer scales with the lead time: at most 3 hours after 09:00 while the device is not in deep idle. A real alert far ahead was measured to carry the same 3 hour window.
- In deep idle (Doze) `setWindow` alarms are not allowed: delivery waits for a maintenance window or for the device to leave idle (the user picks it up), and the reconcile on app start, boot or any product change delivers a due alert. The alert is then later than 3 hours, but never earlier than its moment and never after a reconcile that finds it due.
- API 26 delivered near the alert moment and API 37 delivers at a system-chosen moment inside the window; both are within the product's tolerance of a few hours.
- Every reconcile re-sets the alarms.
- Force stop and OEM battery managers can still delay or drop alarms until the next app start (accepted in spike #38, not testable on emulators).
- On API 33+ the notification is shown only when the permission is granted; the request is `add-notification-permission`.
