## Why

The calendar channel can stop working without the user noticing: Android's unused-app permission reset, or the user, removes calendar access, or the chosen calendar disappears (account removed, calendar deleted or made read-only). `add-calendar-gateway` reports this only on the Settings screen, which the user rarely opens, and `add-calendar-event-sync` then writes nothing. This change (#46 part b2) tells the user on the product list, the screen they use every day, as the disabled-alerts banner already does for notifications. Decided by the owner on 2026-10-16.

## What Changes

- While the calendar channel is on but cannot work, and at least one product exists, the product list shows a banner above the products: "Calendar alerts are off. Calendar access was removed." or "Calendar alerts are off. The chosen calendar is no longer available.", with the button "Fix", which opens the Settings screen where the existing messages offer the fix.
- The banner is checked every time the product list becomes visible and disappears as soon as the problem is fixed. It is not dismissible. When the disabled-alerts banner shows too, both are shown, the notification one first.
- The existing banner layout (button beside the text, or below it when the text needs more than two lines) is reused for both banners, not copied.

No new dependencies.

## Non-goals

- Writing calendar events (`add-calendar-event-sync`, #46 part b1).
- Fixing the problem from the banner itself: "Fix" only opens Settings, which already has "Allow" and the calendar chooser.
- Any change to the Settings screen or to the channel setting: the channel stays on with its stored choice, as `calendar-channel` specifies.
- Spanish texts (#48).

## Capabilities

### New Capabilities
- `calendar-channel-banner`: the product list banner shown while the calendar channel is on but cannot work.

### Modified Capabilities
None.

## Impact

- Depends on: #46 part a (`add-calendar-gateway`, merged). It does not depend on `add-calendar-event-sync` and can run in parallel with it.
- Touches: `:app` only: the `productlist` package (ViewModel, state, screen, banner composable), the navigation from the product list to Settings, string resources, and new product list screenshot references. It does not touch Room, `:domain`, `AppContainer` beyond reading what it already exposes, or the Settings screen.
- Issue: Closes #46 when both b1 and b2 are merged. If this pull request merges before `add-calendar-event-sync`, it must use "Refs #46" instead, and b1 closes the issue.
