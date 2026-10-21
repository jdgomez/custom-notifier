## Why

The CI `e2e` job failed on release PR #84 in `CalendarBannerTest.calendarGoneShowsTheBannerAndTurningTheChannelOffRemovesIt` and passed on the previous head. Cause: `SettingsState.channelOn` starts as `false`, which pretends to be a real value. On a slow device the user (or the test) can tap "Add alerts to my calendar" before the stored setting has loaded, and the tap acts on the default: it asks to turn the channel on instead of turning it off. Decided by the owner on 2026-10-21: fix the app, and harden the e2e tests too.

## What Changes

- Until the stored calendar channel setting has been read, the "Add alerts to my calendar" switch is shown disabled (no spinner, no layout shift). Once read, it shows the real value and is interactive. A tap never acts on a value that is not loaded.
- `SettingsState` models this explicitly: a three-valued `ChannelSwitch` (`Loading`, `Off`, `On`) replaces the boolean default.
- The e2e tests that tap Settings controls wait for the enabled switch before tapping.
- Other screens were checked for the same pattern: the product form already has an explicit `Loading` mode, and the product list has `Loading`, its banners only appear after their values are computed and its controls are the banner buttons, which exist only then. Nothing else changes.

No new dependencies.

## Non-goals

- Any visual change of the loaded Settings screen (screenshot references stay as they are).
- A spinner or skeleton while loading.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `calendar-channel`: the Settings switch is disabled until the stored setting has loaded.

## Impact

- Touches: `settings/` (state, view model, screen) and the instrumented Settings tests.
- Depends on: nothing. Production code is far below the 400 line budget.
