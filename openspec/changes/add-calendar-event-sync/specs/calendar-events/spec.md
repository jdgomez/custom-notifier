## ADDED Requirements

### Requirement: One alert event per product
While the calendar channel is on, calendar access is granted and the chosen calendar is usable, each product whose next alert is scheduled or due SHALL have exactly one alert event in the chosen calendar, and a product whose next alert is none SHALL have no alert event. The next alert SHALL be computed in the device's current time zone, as for alarms. An alert event SHALL stay on its date after its alert has been delivered, until the product's next alert changes.

#### Scenario: Event for a product with a scheduled alert
- **GIVEN** the calendar channel on with the calendar "Home" and a product "Coffee" whose alert is scheduled on 2026-03-01
- **WHEN** the events are synchronized
- **THEN** "Home" has one event "Buy Coffee" on 2026-03-01 and no other event created by the app

#### Scenario: Event kept after the alert is delivered
- **GIVEN** the event "Buy Coffee" on 2026-03-01 and the alert for "Coffee" delivered on 2026-03-01
- **WHEN** the events are synchronized on 2026-03-05, before the depletion date
- **THEN** the event "Buy Coffee" is still on 2026-03-01

#### Scenario: No event after the depletion date
- **GIVEN** the event "Buy Coffee" and a depletion date of 2026-03-11
- **WHEN** the events are synchronized on 2026-03-12
- **THEN** the event "Buy Coffee" is deleted

### Requirement: Shape of an alert event
An alert event SHALL be an all-day event on the local date of the product's alert moment, SHALL NOT have any reminder, and SHALL NOT repeat. Its title SHALL be "Buy <product name>" and its description "Runs out <depletion date>. Added by Custom Notifier.", with the depletion date in the device locale's medium format at the time the event is written. The event SHALL show on the same calendar date whatever the time zone of the device or of the calendar.

#### Scenario: Texts of the event
- **GIVEN** the calendar channel on and a product "Coffee" with an alert on 2026-03-01 and a depletion date of 2026-03-11, in an English locale
- **WHEN** the event is created
- **THEN** it is an all-day event on 2026-03-01 titled "Buy Coffee", described "Runs out Mar 11, 2026. Added by Custom Notifier.", with no reminder

#### Scenario: Date independent of the time zone
- **GIVEN** an all-day event created for an alert on 2026-03-01 while the device was in Europe/Madrid
- **WHEN** the device time zone changes to America/New_York
- **THEN** the calendar still shows the event on 2026-03-01

### Requirement: Events follow product changes
When a product changes so that its alert date, its name or its depletion date change, its alert event SHALL be updated to match. When a product is deleted, its alert event SHALL be deleted. Changes the user made to an alert event in the calendar app SHALL be overwritten on the next synchronization.

#### Scenario: Restock moves the event
- **GIVEN** the event "Buy Coffee" on 2026-03-01
- **WHEN** the user restocks "Coffee" so that its alert moves to 2026-03-31
- **THEN** the event "Buy Coffee" is on 2026-03-31 and no event remains on 2026-03-01

#### Scenario: Rename updates the title
- **GIVEN** the event "Buy Coffee"
- **WHEN** the user renames the product to "Espresso" and saves
- **THEN** the event is titled "Buy Espresso"

#### Scenario: Deleting the product deletes the event
- **GIVEN** the event "Buy Coffee"
- **WHEN** the user deletes the product "Coffee"
- **THEN** the event "Buy Coffee" is deleted

### Requirement: Channel changes create, move and delete events
When the channel is turned on, the events of all products with a next alert SHALL be created at once. When the user chooses another calendar, the events SHALL be deleted from the old calendar when it still exists and created in the new one. When the channel is turned off, every alert event the app created SHALL be deleted, without confirmation.

#### Scenario: Turning the channel on
- **GIVEN** the products "Coffee" and "Rice", both with a next alert, and the channel off
- **WHEN** the user turns the channel on and chooses "Home"
- **THEN** "Home" has the events "Buy Coffee" and "Buy Rice"

#### Scenario: Changing the calendar
- **GIVEN** the channel on with "Home" and the event "Buy Coffee" in "Home"
- **WHEN** the user chooses "Family"
- **THEN** "Family" has the event "Buy Coffee" and "Home" no longer has it

#### Scenario: Turning the channel off
- **GIVEN** the channel on with the events "Buy Coffee" and "Buy Rice"
- **WHEN** the user turns the switch off
- **THEN** both events are deleted and no dialog is shown

### Requirement: Events deleted outside the app
When an alert event no longer exists, or has been deleted, in the calendar where the app created it, the app SHALL NOT recreate it on its own: not at app start, after boot, after a clock or time zone change, or when an alarm fires. It SHALL recreate it only after the user changes that product in the app (saving it from the form, restocking or adjusting it), as long as the product still has a next alert. The app SHALL NOT update or delete any event other than one it created and still finds in that calendar.

#### Scenario: Deleted event stays deleted in the background
- **GIVEN** the event "Buy Coffee", deleted by the user in the calendar app
- **WHEN** the app starts again, or the device reboots
- **THEN** no event "Buy Coffee" is created

#### Scenario: Deleted event comes back after a product change
- **GIVEN** the event "Buy Coffee", deleted by the user in the calendar app
- **WHEN** the user restocks "Coffee"
- **THEN** a new event "Buy Coffee" is created on the new alert date

#### Scenario: Other products are not affected
- **GIVEN** the events "Buy Coffee" and "Buy Rice", and "Buy Coffee" deleted by the user in the calendar app
- **WHEN** the user restocks "Rice"
- **THEN** "Buy Rice" is moved and "Buy Coffee" is not recreated

### Requirement: When events cannot be written
Without calendar access, or while the chosen calendar no longer exists or is no longer writable, the app SHALL write nothing to any calendar and SHALL NOT crash, and SHALL keep what it knows about the events it created. When access is back or the user chooses a usable calendar, the next synchronization SHALL bring the events in line with the requirements above, including deleting events of products deleted meanwhile and creating events for products added meanwhile.

#### Scenario: Product added without calendar access
- **GIVEN** the channel on with "Home" and calendar access removed
- **WHEN** the user creates the product "Rice"
- **THEN** no event is written and the app works as usual

#### Scenario: Access given back
- **GIVEN** the product "Rice" created while calendar access was removed
- **WHEN** the user taps "Allow" in Settings and allows calendar access
- **THEN** "Home" has the event "Buy Rice"

#### Scenario: Product deleted without calendar access
- **GIVEN** the event "Buy Coffee" and calendar access removed
- **WHEN** the user deletes "Coffee", then allows calendar access again
- **THEN** the event "Buy Coffee" is deleted on the next synchronization

### Requirement: When events are synchronized
The app SHALL synchronize alert events after every product change, after every change of the calendar channel setting, after calendar access is granted from the app, when the app process starts, when the device finishes booting, when the app is updated, when the clock or the time zone changes, and when an alarm fires. Synchronizing SHALL be idempotent: running it again without any change SHALL change nothing in the calendar.

#### Scenario: Idempotent synchronization
- **GIVEN** the events "Buy Coffee" and "Buy Rice" up to date
- **WHEN** the app is started twice
- **THEN** the calendar still has exactly those two events, unchanged
