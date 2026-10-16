## Context

See proposal.md - Why. The spike #38 (findings, sections 2.3 and 2.4, decisions 3 and 4) defines the Calendar Provider approach. `add-calendar-gateway` added `CalendarId`, `DeviceCalendar`, `CalendarChannel`, the `CalendarGateway` port (`writableCalendars`, `find`), the `CalendarChannelSettings` port, `ProviderCalendarGateway`, `CalendarAccess` (stateless) and the `calendar_channel` table (database version 3). Alarms are kept right by `AlertReconciler` in `:domain`, run by `AppContainer.startAlerts` on every `ProductRepository.observeAll()` emission and by `AppContainer.reconcileAlerts` from the broadcast receivers (boot, update, clock, time zone, alarm). Every product write goes through `ProductRepository.save` or `delete`, and every caller of those is a user action (form save, restock, adjust, delete).

Owner decisions (2026-10-16): event on the alert date; title "Buy <name>" and description "Runs out <date>. Added by Custom Notifier."; one event per product, moved when the alert date changes and deleted when the next alert is none or the product is deleted; turning the channel on creates all events at once; changing the calendar deletes from the old one and creates in the new one; background syncs only update or delete existing events and create events for products that never had one, never recreating an event the user deleted; the product list banner is a separate change.

## Goals / Non-Goals

**Goals:**
- All decisions about which event to create, update or delete live in `:domain` and are unit-tested without Android.
- One idempotent sync, run at the same triggers as the alarm reconcile.

**Non-Goals:**
- The product list banner (`add-calendar-channel-banner`).
- Observing the calendar provider while the app runs (`ContentObserver`): external deletions are detected lazily at the next sync.

## Decisions

### 1. Domain types and port operations
- `EventId` (value class over the provider's `Long`) and `AlertEvent(productName: ProductName, depletionDate: DepletionDate, date: LocalDate)`, the content the app wants in the calendar for one product. "Alert event" is added to the glossary: the all-day calendar event the app keeps for a product's next alert.
- `CalendarGateway` gains `suspend fun canWrite(): Boolean` (calendar access granted), `suspend fun create(calendar: CalendarId, event: AlertEvent): EventId?`, `suspend fun exists(calendar: CalendarId, id: EventId): Boolean`, `suspend fun update(calendar: CalendarId, id: EventId, event: AlertEvent): Boolean` and `suspend fun delete(calendar: CalendarId, id: EventId)`. `update` returns false when the event is gone. Without access every operation does nothing and returns null or false, as for the existing operations.
- The title and description are presentation (the description's date uses the locale's medium format), so `AlertEvent` holds the name and dates, and the `:app` adapter builds the texts, formatting the date with the same function the notification uses (one shared function, not a copy).

### 2. Stored event records
- A `CalendarEventRecord(productId, calendarId, eventId?, content)` per product, behind a `CalendarEventRecords` port in `:domain` (`all()`, `put`, `remove`). `eventId = null` means "the event was found gone and is waiting for a user change of the product". `content` is the last `AlertEvent` written, so a sync that finds nothing changed writes nothing (idempotent, no provider writes on every app start).
- Room: table `calendar_event` (`product_id` primary key, `calendar_id`, `event_id` nullable, `title_name`, `depletion_date`, `event_date`), no foreign key to products. A cascade would drop the record before the event could be deleted, for example while access is missing. Database version 4 with `MIGRATION_3_4` creating the table, and `app/schemas/4.json` committed.
- Alternative: columns on the product table. Rejected: the record outlives the product until its event is deleted, and it is calendar adapter state, not product data.

### 3. Knowing which products the user changed
A `ProductChanges` record in `:domain`, filled by a `ProductRepository` decorator (`TrackingProductRepository`) that records the id on every `save` and `delete` before delegating. `AppContainer` exposes the decorated repository to the ViewModels. The sync takes and clears the pending ids. Only a pending id clears a "gone" record (`eventId = null`), so the event is recreated.
- Alternative: compare product values with a stored snapshot. Rejected: a change that leaves the event content the same (for example a package size) would not count as a user change, and the rule is "when the user changes the product".
- Alternative: call the sync from each ViewModel. Rejected: easy to miss in a new write path. The decorator covers every write in one place.

### 4. `CalendarEventSync` in `:domain`
`sync(products: List<Product>, channel: CalendarChannel, now: Instant, zone: ZoneId)`, serialized with a `Mutex` like `AlertReconciler`:
1. `canWrite()` false, or channel `On` with a calendar that `find` does not return: do nothing and keep records and pending ids.
2. Channel `Off`: delete every recorded event (`delete` is a no-op when it is gone) and remove the records.
3. Channel `On(c)`: for each record in another calendar, delete its event and remove it. For each record whose product no longer exists or whose next alert is none, delete its event and remove it. For each product with a next alert:
   - with no record, create the event and store it;
   - with a gone record, recreate it only when the product id is pending;
   - with a live record whose content differs, `update`; if `update` returns false, store the record as gone, and when the product is pending create it again at once;
   - with a live record whose content is the same, check `exists` and store the record as gone when it is not.
4. Clear the pending ids that were handled.
`create` returning null (access lost mid-sync) stops the sync without changing records; the next trigger retries.
The event date is the local date of the alert moment in `zone`, also when the next alert is due (late delivery keeps the original alert date).
The `exists` check in step 3 costs one provider query per product per sync. That is acceptable for the expected number of products.

### 5. Provider event operations
In `ProviderCalendarGateway`, on `Dispatchers.IO`:
- `create`: insert into `Events` with `CALENDAR_ID`, `TITLE`, `DESCRIPTION`, `ALL_DAY = 1`, `DTSTART` at midnight UTC of the date, `DTEND` at the next midnight UTC, `EVENT_TIMEZONE = "UTC"`, `HAS_ALARM = 0`. The id is the last segment of the returned URI.
- `exists`: query `Events` with `_ID = ? AND CALENDAR_ID = ? AND DELETED = 0`.
- `update` and `delete`: first the same `exists` check, then `update` or `delete` by URI. An event that is not found is never touched (spike 2.3).
- No reminder is inserted, so the calendar app's default reminder does not apply: `HAS_ALARM = 0` and no `Reminders` row. Verify on `cn-api37` that the stock calendar app shows no reminder.
- A `SecurityException` is caught everywhere and treated as no access.

### 6. Triggers and wiring
`AppContainer` builds `CalendarEventSync` with the provider gateway, `RoomCalendarEventRecords` and `ProductChanges`. It combines `productRepository.observeAll()` with `calendarChannelSettings.observe()` and runs the sync on each emission, after the alarm reconcile, in the same application scope. `reconcileAlerts` (receivers) runs the sync after the reconcile with the stored products and channel, before `onDone`. Calendar access granted in Settings triggers one sync through a `syncCalendar()` entry point on `AppContainer`, called by `SettingsViewModel` after a permission result that is `Granted`. That one-line call is the only change in `settings`.

### 7. Testing
- `:domain` unit tests with fakes for the gateway and records, for every spec scenario, including idempotency (a second sync makes no gateway write calls), changed calendar, off, access missing, product deleted while access is missing, and a gone event recreated only for a pending id.
- Room tests for `RoomCalendarEventRecords` and a migration test from the exported version 3 schema.
- Instrumented tests on `cn-api37` against a local calendar created by the test (`ACCOUNT_TYPE_LOCAL`, as in part a): create and check the all-day fields, date stays right after `cmd time_zone_detector` or a zone change, update, delete, an event deleted from the provider by the test is not recreated at app start but is after a restock, channel off deletes all, no reminder rows.
- No new screenshots: no UI change.

## Risks / Trade-offs

- [All-day dates shifting by one day] → UTC midnight with `EVENT_TIMEZONE = "UTC"`, tested with a zone change on the emulator and in unit tests of the date computation.
- [The calendar app adds a default reminder to new events] → `HAS_ALARM = 0` and no reminder rows; checked by hand on `cn-api37` with the stock calendar app. If an OEM still adds one, it is outside the app's control and noted.
- [The user edits the event's date in the calendar app] → Overwritten on the next sync that finds the content changed in the app. The app does not compare the provider's fields with what it wrote, so a user edit lasts until the product changes. Accepted: the app is the source of truth only for what it writes.
- [Size close to 400 production lines] → The banner is already split off. If the count clearly exceeds the limit, the executor stops and reports before going on.

## Migration Plan

Room migration 3 to 4 adds an empty table. Installs with the channel already on get their events at the first sync after the update (the `MY_PACKAGE_REPLACED` trigger or the next start). Rollback is reverting the PR before any release.
