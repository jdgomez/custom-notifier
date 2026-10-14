## Context

See proposal.md - Why. The spike findings for #38 (section 2 and decisions in section 5) define the Calendar Provider approach: `READ_CALENDAR` + `WRITE_CALENDAR` asked only at opt-in, writable and visible calendars only, an empty state, no intent fallback. Owner decisions for this change (2026-10-14): one app-wide channel with one calendar; a new Settings screen reached from the product list top bar; a dialog chooser showing name and account; this change merges before part b even though it creates no events yet; in part b, turning the switch off deletes the created events at once without confirmation (a confirmation dialog is post-MVP).

Current state: `:domain` has no channel concept (the push channel is always on). `:app` uses manual DI in `AppContainer` (ADR 0015), Room 3 at database version 2 (`MIGRATION_1_2`), AndroidX Navigation 3 with an in-memory back stack (ADR 0016, destinations `ProductList` and `NewProduct` plus edit), and the notification permission flow of `add-notification-permission` (a `NotificationAccess` adapter plus a `SharedPreferences` flag for permanent denial; removed in review round 3, see decision 4).

## Goals / Non-Goals

**Goals:**
- A domain port shaped so part b only adds event operations to it.
- The calendar permission flow mirrors the notification permission flow, so both behave and are tested the same way.

**Non-Goals:**
- Any event writing, per-product event ids, or the product list banner (part b).
- A general settings framework: the screen has one section; more sections are added when needed.

## Decisions

### 1. Domain types and the `CalendarGateway` port
`:domain` gets:
- `CalendarId` (value class over the provider's `Long` id) and `DeviceCalendar(id: CalendarId, name: String, accountName: String)`. "Device calendar" is added to the glossary in `docs/SESSION0.md`; it avoids clashing with `java.util.Calendar` and says where the calendar lives.
- `CalendarChannel`: sealed, `Off` or `On(calendarId: CalendarId)`.
- `CalendarGateway` port: `suspend fun writableCalendars(): List<DeviceCalendar>` and `suspend fun find(id: CalendarId): DeviceCalendar?` (null when the calendar is gone or no longer writable). Both throw nothing for missing access: the adapter returns an empty list or null and the caller checks access separately (decision 4). Part b adds event operations to this port.
- `CalendarChannelSettings` port: `fun observe(): Flow<CalendarChannel>` and `suspend fun set(channel: CalendarChannel)`.
- Alternative: one port for both provider access and the stored setting. Rejected: they have different adapters (Calendar Provider vs Room) and different lifetimes.
- Alternative: keep everything in `:app`, like `NotificationAccess`. Rejected: part b's sync logic (which event to create, move or delete for which product) is domain logic and needs these types and the port in `:domain`.

### 2. Calendar Provider adapter
`ProviderCalendarGateway` in a new `:app` package `calendar` queries `CalendarContract.Calendars` on `Dispatchers.IO` with the selection `VISIBLE = 1 AND SYNC_EVENTS = 1 AND CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR`, projecting `_ID`, `CALENDAR_DISPLAY_NAME`, `ACCOUNT_NAME`, sorted by account then name. `find` uses the same selection plus `_ID = ?`. A `SecurityException` (access revoked between check and query) is caught and treated as "no calendars" / null.

### 3. Storing the setting in Room
A single-row table `calendar_channel` (`id` fixed to 0, `calendar_id INTEGER` nullable: null means off), entity, DAO with `Flow` and upsert, `RoomCalendarChannelSettings` adapter. Database version 3 with `MIGRATION_2_3` creating the table (no row means off) and `app/schemas/3.json` committed.
- Alternative: `SharedPreferences` (already used for UI flags). Rejected: the setting is product data that part b reads together with per-product event rows in the same database, and Room gives a `Flow` and migration-tested schema for free. UI-only flags (decision 4) stay in `SharedPreferences`.
- Alternative: DataStore. Rejected: new dependency.

### 4. `CalendarAccess`, stateless
A `CalendarAccess` class in the `calendar` package reports `Granted` or `Missing`, read from the platform on every call: nothing about denials is stored, because Android gives no reliable "denied for good" signal and a stored guess went stale (the user resets the permission in the system settings, or Android answers without a dialog). Both permissions are requested together with `RequestMultiplePermissions`; access is `Granted` only when both are granted, re-read after the result (the result map is never trusted, and a partial grant is `Missing`). Exposed by `AppContainer` together with the gateway and the settings.
- The switch (or "Allow") always launches the system request, after "Allow calendar access?" only when `shouldShowRequestPermissionRationale` is true for either permission.
- After a result that is not `Granted`, when no rationale is due the "Calendar access is off" dialog shows at once, whose "Open settings" uses `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` for the app package; when a rationale is due, the switch just stays off and the next attempt explains first.
- The notification permission follows the same stateless rule: after a save a denied request opens nothing; "Turn on" always asks, then opens the notification settings when the result is denied and no rationale is due.
- `SettingsViewModel` runs every state-writing coroutine one at a time and in request order, each reading the access and the calendars when it runs, so the last request has the last word and none is lost.

### 5. Settings screen and ViewModel
A new Navigation 3 destination `Settings`, pushed by a gear `IconButton` in the product list `TopAppBar` actions (the icon goes into `AppIcons` like the existing ones, no new dependency). `SettingsViewModel` combines `CalendarChannelSettings.observe()` with a re-check of `CalendarAccess` and `CalendarGateway.find` done on `ON_RESUME` and after each permission result, and exposes one state: switch on/off, the calendar row (calendar, or "no longer available"), the access-removed message, and which dialog is open (explanation, denied for good, chooser with its calendars, no calendar available). Turning the switch on never stores `On` by itself: only choosing a calendar does. Turning it off stores `Off` at once. "Allow" re-asks; after access is granted it opens the chooser only when `find` returns null.
- Material 3 components only: `ListItem` with a trailing `Switch` (the whole row toggles), `AlertDialog` with a `LazyColumn` of `RadioButton` rows for the chooser, as in the existing form dialogs.

### 6. Testing
- `:domain`: no logic beyond types; covered through the ViewModel and adapter tests.
- Robolectric: `ProviderCalendarGateway` against a fake provider is not practical, so the adapter is covered by instrumented tests; `RoomCalendarChannelSettings` and `MIGRATION_2_3` by Room tests (migration test from the exported version 2 schema); `CalendarAccess` states like `NotificationAccess`; `SettingsViewModel` with fakes for every spec scenario; Compose UI tests for the screen and dialogs.
- Screenshot references: Settings screen off, on, on with access removed, on with calendar gone; chooser; no calendar available; explanation and denied-for-good dialogs; each in light and dark, the screen also at the largest font size.
- Instrumented (UiAutomator for the system dialog) on `cn-api37`: the test creates local calendars itself through the provider as a sync adapter with `ACCOUNT_TYPE_LOCAL` (a fresh emulator has no account), including one read-only calendar, and removes them afterwards; scenarios: first time on and grant then choose, deny, denied for good opens app settings, no calendar available, read-only not listed, channel kept after relaunch, calendar removed shows the message, access revoked with `pm revoke` shows the message.

## Risks / Trade-offs

- [The app creates no events until part b] → Accepted by the owner; no external users before #47. Part b is the next change.
- [OEM calendar apps expose calendars with unusual flags (for example `SYNC_EVENTS = 0` for local calendars)] → The selection follows the spike; verify on `cn-api26` and `cn-api37`, and widen it in part b only with evidence.
- [A dismissed first dialog is indistinguishable from a denial for good] → Accepted: the "Calendar access is off" dialog may show once, and the next attempt asks again because nothing is stored.
- [Android's unused-app permission reset removes access silently] → The Settings screen reports it on the next visit; part b adds the product list banner.
- [Size close to 400 production lines (adapter, Room pieces, access, screen, ViewModel, dialogs)] → Keep the screen to one section and reuse existing dialog patterns; if it clearly exceeds the limit, the executor stops and reports so the split can be decided.

## Migration Plan

Room migration 2 to 3 adds an empty table; existing installs start with the channel off. Rollback is reverting the PR before any release (no released builds exist yet).
