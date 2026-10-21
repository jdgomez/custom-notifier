# Privacy policy - Custom Notifier

Last updated: 2026-10-21. [Versión en español](privacy-policy.es.md)

Custom Notifier is an Android app that alerts you before a household consumable runs out. It is made by an individual developer ("the developer").

## Short version

All your data stays on your device. The app has no account, no analytics, no advertising and no server. The developer receives no data from the app and shares nothing with anyone.

## What the app stores

The products you enter (name, unit, package size, consumption, alert lead time and stock) are stored only in the app's private storage on your device. Android backup and device transfer are turned off for the app, so this data is not copied to any cloud by the app. Uninstalling the app deletes it.

## Permissions and what they are used for

- **Notifications (`POST_NOTIFICATIONS`).** To show you one local notification before a product runs out. Notifications are created on your device; nothing is sent anywhere. The app asks for this permission after you save a product.
- **Calendar read and write (`READ_CALENDAR`, `WRITE_CALENDAR`).** Only if you turn on "Add alerts to my calendar" in Settings, and only after you allow it. The app lists the calendars on your device so you can choose one, and writes one all-day event per alert (titled "Buy <product>", with the date it runs out) into the calendar you chose, and reads back its own events to keep them up to date. It does not read your other events for any other purpose. If you do not turn this on, the app never touches your calendar. You can turn it off in Settings or remove the permission in the system settings at any time.
- **Run after restart (`RECEIVE_BOOT_COMPLETED`).** To schedule your alerts again after the device restarts. This sends no data.

## Calendar services outside the app

The events the app writes live in the calendar you chose. If that calendar is synchronized by an account on your device (for example a Google account), the calendar app and that account provider synchronize the events under their own privacy policy. The app does not control this and does not send the events anywhere itself. Choose a calendar that is local to the device if you do not want them synchronized.

## What the app does not do

The app has no internet permission. It collects no personal data, uses no analytics or crash-reporting services, shows no ads, has no account and shares no data with third parties.

## Children

The app is a general household tool and is not directed at children.

## Changes

If this policy changes, the new version is published at this address with a new date.

## Contact

Open an issue at <https://github.com/jdgomez/custom-notifier/issues>.
