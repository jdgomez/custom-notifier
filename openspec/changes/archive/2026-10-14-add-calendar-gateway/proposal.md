## Why

SESSION0 promises two alert channels: the push notification (done in #45) and an event in the calendar the user already uses, opt-in and behind explicit calendar permission. Before events can be written (`add-calendar-event-sync`, #46 part b), the user needs a way to turn the calendar channel on, grant calendar access and choose which calendar receives the events. This change is #46 part a; the spike decisions are in the issue #38 findings, section 5.

## What Changes

- A new Settings screen, reached from a settings action in the product list top bar, with a "Calendar" section: a switch "Add alerts to my calendar" and, while it is on, a row showing the chosen calendar that opens the chooser to change it.
- One app-wide calendar channel setting (off, or on with one chosen calendar) for all products, stored on the device. Off by default.
- Turning the switch on asks for calendar access (`READ_CALENDAR` and `WRITE_CALENDAR`, one system dialog) only at that moment, never at launch, with an explanation first when Android reports one should be shown, and a dialog leading to the app's system settings when Android no longer shows the request. If access is denied the switch stays off: no intent-based fallback.
- A chooser dialog listing the writable, visible calendars on the device (name and account), and an empty state when there is none, after which the switch stays off.
- While the channel is on, the Settings screen tells the user when calendar access was removed (for example by Android's unused-app permission reset) or when the chosen calendar no longer exists, and offers the fix.
- A `CalendarGateway` port in `:domain` (list writable calendars, check that a calendar is still usable) with a Calendar Provider adapter in `:app`. Part b extends the same port with event operations.
- The setting is stored in the existing Room database: a new single-row table, database version 3 with a migration from version 2.
- Glossary: a new term "Device calendar" in `docs/SESSION0.md`, the type name for a calendar on the device that can receive events.

Between this change and part b, turning the channel on stores the choice but creates no events yet. Accepted by the owner: nobody outside the project uses the app before #47.

No new dependencies. The calendar permissions are new manifest permissions; their Play Data safety entry is already an owner step of #47.

## Non-goals

- Creating, moving or deleting calendar events, storing event ids per product, and the product list banner when the calendar is gone: all part b (`add-calendar-event-sync`).
- Choosing the channel or the calendar per product (possible later without rework: part b stores the calendar per product).
- Turning the push channel off or any other setting on the new screen.
- An "add to calendar" intent fallback when access is denied (spike decision 2).
- Spanish texts (#48).

## Capabilities

### New Capabilities
- `calendar-channel`: the Settings screen and its entry point, turning the calendar channel on and off, the calendar permission flow, the calendar chooser and its empty state, and how the screen reports lost access or a missing calendar.

### Modified Capabilities
- `notification-permission`: no stored "Android no longer allows asking" state; a denied or silent request after a save shows only the banner, and "Turn on" always asks, then opens the notification settings when Android would not explain again (the same stateless rule as the calendar permission, review round 3).

## Impact

- Depends on: #45 (`add-alert-scheduler`, `add-notification-permission`; both merged). Part b depends on this change.
- Touches: `:domain` (calendar port and channel types), `:app` (calendar adapter, Room entity, DAO and migration 2 to 3 with `app/schemas/3.json`, manifest permissions, new `settings` package with screen and ViewModel, navigation destination, product list top bar action), `docs/SESSION0.md` (glossary term). Not parallelizable with part b; parallelizable with work that does not touch navigation, the product list top bar or the Room schema.
- Issue: Refs #46, which must stay open until part b.

## Handoff note

Size: this change is about 480 production code lines (about 710 raw), over the usual 400. The owner accepted it as a one-off exception. The pull request body must state this exception, to be raised at the Phase 1 retrospective.
