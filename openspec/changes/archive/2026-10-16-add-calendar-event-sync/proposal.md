## Why

SESSION0 promises that each alert can also appear in the calendar the user already uses, and that calendar events are created, moved and deleted in sync with changes in the app. `add-calendar-gateway` (#46 part a) lets the user turn the calendar channel on and choose a calendar, but no event is written yet. This change (#46 part b1) writes the events. The decisions come from the spike #38 (findings, section 5) and from the owner's planning on 2026-10-16.

## What Changes

- While the calendar channel is on and calendar access is granted, every product with a next alert gets exactly one all-day event, with no reminder, in the chosen calendar, on the local date of its alert. The title is "Buy <product name>" and the description "Runs out <depletion date>. Added by Custom Notifier.".
- Each event follows its product. It moves or is rewritten when the alert date, the name or the depletion date change. It is deleted when the product is deleted or when the product has no next alert any more (after its depletion date).
- Turning the channel on creates the events at once. Changing the calendar deletes the events from the old calendar, when that calendar still exists, and creates them in the new one. Turning the channel off deletes every event the app created, without confirmation.
- An event the user deleted in the calendar app is not recreated in the background. It is recreated only when the user changes that product again in the app (save, restock or adjust).
- Events are synchronized at the same moments as alarms: after every product change, at app start, after boot and app update, after clock and time zone changes, when an alarm fires, and after every change of the channel setting.
- Without calendar access, or while the chosen calendar is gone, nothing is written and the stored event records are kept, so the next sync with access puts things right.
- The app stores, per product, the calendar and the event it created, in the existing Room database: a new table, database version 4 with a migration from version 3. Before every update or delete the app checks that the event still exists in that calendar and has not been deleted.
- The `CalendarGateway` port gets event operations. A new domain service decides which event to create, move or delete for each product.
- Glossary: a new term "Alert event" in `docs/SESSION0.md`.

No new dependencies.

## Non-goals

- The product list banner when the calendar channel cannot work (no access, or calendar gone): `add-calendar-channel-banner` (#46 part b2).
- A confirmation before deleting events when the channel is turned off (post-MVP, #74).
- Choosing the channel or the calendar per product.
- Keeping past events as history: there is at most one event per product.
- Reading changes the user makes to the event in the calendar app: the app is the source of truth and overwrites them on its next sync.
- Spanish texts (#48).

## Capabilities

### New Capabilities
- `calendar-events`: which alert events exist, their shape and texts, when they are created, moved and deleted, how external deletions and missing access or calendars are handled.

### Modified Capabilities
None. The Settings behavior of `calendar-channel` does not change; the deletion caused by turning the channel off is specified in `calendar-events`.

## Impact

- Depends on: #46 part a (`add-calendar-gateway`, merged) and #45 (`add-alert-scheduler`, merged, for the shared triggers).
- Touches: `:domain` (event types, port operations, sync service, product change tracking), `:app` (Calendar Provider event operations in the `calendar` package, Room entity, DAO and migration 3 to 4 with `app/schemas/4.json`, `AppContainer` wiring), `docs/SESSION0.md` (glossary term). It does not touch the product list or Settings UI. Parallelizable with `add-calendar-channel-banner`, which touches only the product list and its ViewModel.
- Issue: Refs #46, which must stay open until part b2.
