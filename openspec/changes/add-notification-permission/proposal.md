## Why

Alerts are scheduled and posted (`add-alert-scheduler`, #45 part a), but on Android 13 and later a notification is shown only if the user granted `POST_NOTIFICATIONS`, and the app never asks for it. On any version the user can also turn notifications off for the app or the "Depletion alerts" channel. In both cases the app silently fails its core promise. This change asks for the permission in context and keeps the user aware while alerts cannot be shown (issue #45 part b).

## What Changes

- On Android 13 and later, after the user saves a product (new or edited) while the notification permission is not granted, the app asks for it on the product list, as long as Android still allows asking. A dismissed request is asked again on the next save.
- The app shows its own explanation before the system dialog only when Android reports that one should be shown (after an earlier denial).
- A disabled-alerts banner at the top of the product list while alerts cannot be shown (permission not granted, app notifications off, or the "Depletion alerts" channel off), on every supported version, shown only when at least one product exists. Its action asks for the permission when Android allows it, and otherwise opens the system notification settings of the app (or of the channel). It cannot be dismissed while alerts stay off and disappears as soon as they are back on.
- Alarms keep being scheduled while notifications are off; the display-time check of `add-alert-scheduler` stays as it is, so a due alert is shown once notifications are allowed again (until its depletion date).

No new dependencies.

## Non-goals

- Changes to scheduling, delivery or the notification itself (`alert-delivery` stays unchanged).
- Asking at first launch or from an onboarding screen.
- Exact alarms, calendar events (#46), Spanish texts (#48).
- Detecting a revocation made in system settings while the app is not running beyond what the banner shows on the next visit.

## Capabilities

### New Capabilities
- `notification-permission`: when the app asks for the notification permission, the explanation shown before asking, and the disabled-alerts banner with its action.

### Modified Capabilities
- (none)

## Impact

- Depends on: `add-alert-scheduler` (#45 part a, done).
- Touches: `:app` only (product list screen and ViewModel, a notification access adapter next to the alerts code, the permission request wiring after saving, string resources, Robolectric and screenshot tests with new references, instrumented E2E tests on API 37 and the API 26 check). No `:domain`, Room or manifest changes (`POST_NOTIFICATIONS` is already declared).
- Not parallelizable with other changes to the product list screen or the save flow.
