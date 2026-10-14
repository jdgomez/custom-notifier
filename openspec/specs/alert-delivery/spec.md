## Purpose

Makes the app actually alert the user: each product's next alert is scheduled with the system, delivered exactly once as a local notification, delivered late when it was missed, and kept correct across reboots, updates, clock and time zone changes and every change to the product.

## Requirements

### Requirement: One alert per product, delivered once
For each stored product, the app SHALL act on its next alert, computed from the current moment in the device's current time zone, and on whether its alert has been delivered:
- **Scheduled**: the app SHALL have exactly one pending system alarm for the product at that moment, and SHALL clear the product's delivery record.
- **Due** and not delivered: the app SHALL post the product's notification and record the delivery.
- **Due** and already delivered: the app SHALL do nothing.
- **None**: the app SHALL have no pending alarm for the product and SHALL post nothing.
A product SHALL therefore get a new alert only after its next alert has become scheduled again (for example after a restock moves the depletion date later).

#### Scenario: Alarm at the alert moment
- **GIVEN** a stored product whose next alert is scheduled at 2026-03-01 09:00 in Europe/Madrid, on a device in Europe/Madrid
- **WHEN** the app reconciles alerts
- **THEN** one alarm is pending for the product at 2026-03-01 09:00 in Europe/Madrid and no notification is posted

#### Scenario: Delivered exactly once
- **GIVEN** a stored product whose next alert is due and not delivered
- **WHEN** the app reconciles alerts twice
- **THEN** exactly one notification is posted for the product

#### Scenario: A restock re-arms the alert
- **GIVEN** a product whose alert was delivered
- **WHEN** the user restocks it so that its next alert is scheduled again
- **THEN** its delivery record is cleared and an alarm is pending at the new alert moment

#### Scenario: Small correction after delivery
- **GIVEN** a product whose alert was delivered and is still due
- **WHEN** the user removes 1 unit and its next alert stays due
- **THEN** no new notification is posted

#### Scenario: Past the depletion date
- **GIVEN** a stored product whose depletion date is before today
- **WHEN** the app reconciles alerts
- **THEN** no alarm is pending for it and no notification is posted

### Requirement: Inexact alarms only
Alarms SHALL be inexact with a delivery window that starts at the alert moment and lasts 3 hours, independent of how far ahead they are set, and SHALL wake the device. In deep idle (Doze) the system defers such an alarm until a maintenance window or until the device leaves idle, so the delivery can be later than the window. The app SHALL NOT declare or request any exact-alarm permission and SHALL NOT poll in the background.

#### Scenario: Bounded delivery window
- **GIVEN** a product whose next alert is scheduled at 2026-03-01 09:00, set days ahead, on a device that is not in deep idle
- **WHEN** the alarm is registered
- **THEN** it is delivered no earlier than 2026-03-01 09:00 and at most 3 hours after, whatever the lead time

#### Scenario: Deep idle
- **GIVEN** a product whose alarm moment passes while the device is in deep idle
- **WHEN** the device leaves deep idle, or the app is opened or started
- **THEN** the alert is delivered then, late, by the system or by the reconcile, and never before its alert moment

#### Scenario: No exact-alarm permission
- **GIVEN** the app's merged manifest
- **WHEN** its permissions are listed
- **THEN** neither `SCHEDULE_EXACT_ALARM` nor `USE_EXACT_ALARM` is present

### Requirement: Reconciling keeps alarms correct
The app SHALL reconcile every product's alert, as defined in "One alert per product, delivered once", when the device finishes booting, when the app is updated, when the time zone or the clock changes, when the app process starts, when a product's alarm fires, and after any product is created, edited, restocked, adjusted or deleted. Reconciling SHALL be idempotent. A deleted product SHALL have no pending alarm, and an alarm that fires for a product that no longer exists SHALL post nothing.

#### Scenario: Reboot
- **GIVEN** a product with a scheduled alert
- **WHEN** the device reboots
- **THEN** after boot its alarm is pending again at the same alert moment

#### Scenario: Missed while the device was off
- **GIVEN** a product whose alert moment passed while the device was off, and its depletion date is not yet past
- **WHEN** the device boots
- **THEN** its notification is posted

#### Scenario: Time zone change
- **GIVEN** a product whose next alert is scheduled at 09:00 local time on a device in Europe/Madrid
- **WHEN** the device time zone changes to America/New_York
- **THEN** its alarm is pending at 09:00 in America/New_York on the same alert date

#### Scenario: Editing the lead time moves the alarm
- **GIVEN** a product with a scheduled alert
- **WHEN** the user saves a lead time 2 days longer
- **THEN** its alarm is pending 2 days earlier

#### Scenario: Deleting cancels the alarm
- **GIVEN** a product with a scheduled alert
- **WHEN** the user deletes it
- **THEN** no alarm is pending for it

### Requirement: The notification
The app SHALL create, when it starts, a notification channel named "Depletion alerts" with default importance. A product's notification SHALL be posted on that channel with the title "Buy <product name>" and the text "Runs out <depletion date>", the date in the device locale's medium format, or "Runs out today" when the depletion date is today. Each product SHALL have its own notification, replacing an earlier one for the same product. Tapping it SHALL open the app on the product list and dismiss it. When notifications are disabled for the app or the channel, the app SHALL post nothing and SHALL NOT record the delivery.

#### Scenario: Notification texts
- **GIVEN** a device with an English (United States) locale and a product named "Vitamin D" whose depletion date is 2026-03-11 and whose alert is due
- **WHEN** the app reconciles alerts
- **THEN** a notification titled "Buy Vitamin D" with the text "Runs out Mar 11, 2026" is posted on the "Depletion alerts" channel

#### Scenario: Tapping the notification
- **GIVEN** a posted notification for a product
- **WHEN** the user taps it
- **THEN** the app opens on the product list and the notification is dismissed

#### Scenario: Notifications disabled
- **GIVEN** notifications are disabled for the app and a product's alert is due
- **WHEN** the app reconciles alerts
- **THEN** nothing is posted and the alert is still not recorded as delivered
