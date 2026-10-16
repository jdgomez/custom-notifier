## Context

See proposal.md - Why. The product list already shows the disabled-alerts banner (`productlist/AlertsOff.kt`, `AlertsOffBanner`): `ProductListViewModel` re-reads `NotificationAccess` on `ON_RESUME` (`refreshAccess`) and sets `alertsOff` in `ProductListState.Products`. `AppContainer` already exposes `calendarChannelSettings`, `calendarAccess` and `calendarGateway` (`find`), which the Settings screen uses for the same check (`calendar-channel`, "Reporting a channel that cannot work").

## Goals / Non-Goals

**Goals:**
- One banner composable for both banners; the two-line layout rule lives in one place.
- The product list and the Settings screen judge "the channel cannot work" by the same rule, from the same inputs.

**Non-Goals:**
- Writing or checking calendar events (`add-calendar-event-sync`).

## Decisions

### 1. Shared banner composable
`AlertsOffBanner` becomes a generic `Banner(text, actionLabel, onAction)` in `productlist` (or `ui`, if the Settings screen ever needs it). Both banners call it. The existing disabled-alerts screenshots must stay pixel-identical: the refactor changes no pixels.

### 2. Channel problem in the ViewModel
`ProductListState.Products` gets `calendarProblem: CalendarProblem?` (`AccessRemoved`, `CalendarGone`). On `ON_RESUME` (the existing `refreshAccess`) and on every emission of `calendarChannelSettings.observe()`, the ViewModel computes it: channel `Off` gives null; `On(id)` without `CalendarAccess.Granted` gives `AccessRemoved`; with access and `calendarGateway.find(id) == null` gives `CalendarGone`; otherwise null. The rule is a small function shared with `SettingsViewModel`, which computes the same thing for its messages. Moving it there is allowed only if it is a pure move with no behavior change for Settings.
- Alternative: check only at app start. Rejected: the spec requires a check every time the list becomes visible, as for notifications.

### 3. Navigation
"Fix" calls the same navigation callback as the gear action in the top bar (push `Settings`). Back from Settings returns to the list, whose `ON_RESUME` re-checks.

### 4. Testing
- ViewModel unit tests with fakes for every combination in the spec: off, on and working, access removed, calendar gone, no products, and the re-check on resume.
- Robolectric Compose tests: banner texts, "Fix" opens Settings, both banners in order.
- Screenshot references: list with each calendar banner, and with both banners, in light and dark and at the largest font size. Existing references unchanged.
- Instrumented test on `cn-api37`: channel on with a test-created local calendar, `pm revoke` of the calendar permissions, relaunch, banner shown, "Fix" opens Settings; and the calendar deleted by the test shows the calendar-gone banner.

## Risks / Trade-offs

- [`find` queries the provider on every resume] → One cheap query on `Dispatchers.IO`, only while the channel is on and products exist.
- [Parallel flow `add-calendar-event-sync` also edits `SettingsViewModel` (one call) and `AGENTS.md`] → Small, independent edits; whichever merges second rebases.

## Migration Plan

No data change. Rollback is reverting the PR.
