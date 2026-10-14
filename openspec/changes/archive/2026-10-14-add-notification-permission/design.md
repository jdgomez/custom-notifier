## Context

`add-alert-scheduler` declares `POST_NOTIFICATIONS`, creates the "Depletion alerts" channel at start, and posts nothing (without recording the delivery) while notifications are disabled, so a missed alert is retried on the next reconcile. The reconcile runs on app start and every product change, wired through `AppContainer`. The product list is a Compose screen backed by `ProductListViewModel`; the form returns to the list after "Save". minSdk 26, targetSdk 37. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One place that answers "can alerts be shown, and if not, why" for the UI.
- Ask in context, never at launch, and never in a loop the user cannot escape.
- Behavior verifiable by Robolectric tests, screenshots and E2E on `cn-api37`, plus the API 26 check.

**Non-Goals:**
- Any change to `:domain`, Room, the manifest or the alert delivery rules.
- Notification preferences inside the app.

## Decisions

### 1. A `NotificationAccess` adapter in `:app`
A small class next to the alerts code reports one of: `Allowed`, `PermissionMissing(canAsk)`, `AppNotificationsOff`, `ChannelOff`. It reads `NotificationManagerCompat.areNotificationsEnabled()`, the permission state on API 33+, and the importance of the "Depletion alerts" channel (`IMPORTANCE_NONE` means off). It is UI and platform state, not domain logic, so it stays out of `:domain` and is exposed by `AppContainer`. The ViewModel combines it with "has products" to decide whether the banner shows.
- Alternative: a domain port. Rejected: nothing in the domain decides on it; the only rule (banner only with products) is presentation.

### 2. "Android still allows asking" without a library
Android gives no direct "permanently denied" signal: `shouldShowRequestPermissionRationale` is false both before the first request and after the final denial. The app stores one boolean in plain `SharedPreferences` (platform API, no new dependency): `permanentlyDenied`. After a request result of "denied", it is set to true only if the explanation flag was true before the request and is false after it (the second denial, after which Android stops showing the dialog). A dismissal of the first dialog leaves both flags false, so the app asks again on the next save (owner decision). The stored value is cleared whenever the permission is found granted.
- Alternative: Accompanist permissions. Rejected: new dependency, deprecated in favor of the platform API, and it does not solve the permanent-denial question either.
- Alternative: DataStore. Rejected: new dependency for one boolean.

### 3. Request from the list after saving
The form signals "a product was saved" on its way back to the list (a one-shot flag in the list's state holder, not a navigation argument). The list screen, using `rememberLauncherForActivityResult(RequestPermission())`, then asks if `NotificationAccess` reports `PermissionMissing(canAsk = true)`, showing the "Allow alerts?" dialog first only when `shouldShowRequestPermissionRationale` is true. The banner's "Turn on" uses the same launcher without the explanation dialog. Asking on the list means the user sees the saved product behind the system dialog, and only one screen owns the launcher.
- Alternative: asking on the form before saving. Rejected: it interrupts the save and the user has not seen the result yet.

### 4. Re-checking on every visit and reconciling when allowed
The list re-reads `NotificationAccess` on `ON_RESUME` (returning from settings or from the system dialog) and after every permission result. When the state changes from not allowed to `Allowed`, the app runs the existing reconcile, so a due alert appears without restarting the app.

### 5. Settings intents
`Settings.ACTION_APP_NOTIFICATION_SETTINGS` (API 26+) with `EXTRA_APP_PACKAGE` for the app; `Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS` with `EXTRA_CHANNEL_ID` when only the channel is off.

### 6. Banner placement and look
A Material 3 card-like row above the list content, outside the scrolling list so it stays visible, using the theme's error-container colors, with the text and a text button "Turn on" on the right (wrapping below the text at large font sizes). Texts are in `strings.xml` (Spanish later in #48). Screenshot references: light, dark, and largest font size.

## Risks / Trade-offs

- [The stored `permanentlyDenied` can be stale after the user changes the permission in system settings] → It is cleared when the permission is found granted; if the user revokes it in settings, Android resets the denial count while our flag may still say "permanent", so "Turn on" opens settings instead of the dialog. Settings still lets the user turn alerts on, so the user is never stuck.
- [Dismissal vs. denial behavior differs across Android versions or OEMs] → The E2E covers grant, deny, and dismiss on API 37; the rule only affects whether the next attempt shows the dialog or opens settings, both of which lead to a working path.
- [E2E driving system dialogs is fragile] → Use UiAutomator for the system dialog only, with the shared 30 s UI timeout; reset the permission per test with Orchestrator's clean state and `pm revoke` / `pm clear-permission-flags` in setup.
