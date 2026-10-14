# notification-permission Specification

## Purpose
How the app gets permission to show alerts and keeps the user aware while alerts cannot be shown: the in-context permission request, its explanation, and the disabled-alerts banner.

## Requirements

### Requirement: Asking after saving a product
On Android 13 and later, when the user saves a product (new or edited) and the app is not allowed to post notifications, the app SHALL ask for the notification permission once the product list is shown, as long as Android shows its dialog. The app SHALL NOT ask at launch or at any other moment. When the user dismisses the system dialog without choosing, the app SHALL ask again on the next save. Nothing about earlier denials is stored. When the request after a save is denied, or Android answers it without a dialog, nothing else happens: only the disabled-alerts banner shows, and the app never opens the system settings by itself. On Android 12 and earlier the app SHALL NOT ask for anything.

#### Scenario: First product saved
- **GIVEN** Android 13 or later, the permission never asked, and an empty product list
- **WHEN** the user saves the product "Vitamin D"
- **THEN** the product list shows "Vitamin D" and the system dialog asking to allow notifications appears over it

#### Scenario: Permission granted
- **GIVEN** the system dialog shown after saving a product
- **WHEN** the user allows notifications
- **THEN** the dialog closes, the list shows no disabled-alerts banner, and saving another product asks nothing

#### Scenario: Dialog dismissed
- **GIVEN** the system dialog shown after saving a product
- **WHEN** the user dismisses it without choosing and later saves another product
- **THEN** the permission is asked again

#### Scenario: No asking at launch
- **GIVEN** Android 13 or later and the permission not granted
- **WHEN** the user opens the app
- **THEN** no permission dialog is shown

#### Scenario: Older Android
- **GIVEN** Android 12 or earlier
- **WHEN** the user saves a product
- **THEN** no permission dialog is shown

### Requirement: Explanation before asking again
When Android reports that the app should explain why it needs the permission (after an earlier denial), the app SHALL show, before the system dialog, a dialog titled "Allow alerts?" with the text "Custom Notifier shows one notification before a product runs out, so you can buy more in time. Without it you get no alerts." and the buttons "Continue" and "Not now". "Continue" SHALL show the system dialog; "Not now" SHALL close the explanation without asking. When Android does not report it, the system dialog SHALL be shown directly.

#### Scenario: Asking after an earlier denial
- **GIVEN** the user denied the permission once and Android reports that an explanation should be shown
- **WHEN** the user saves a product
- **THEN** the "Allow alerts?" dialog is shown on the product list, and tapping "Continue" shows the system dialog

#### Scenario: Not now
- **GIVEN** the "Allow alerts?" dialog
- **WHEN** the user taps "Not now"
- **THEN** the dialog closes, no system dialog is shown, and the disabled-alerts banner is shown

### Requirement: Disabled-alerts banner
While the app cannot show alerts (notification permission not granted, notifications turned off for the app, or the "Depletion alerts" channel turned off) and at least one product exists, the product list SHALL show, above the products, a banner with the text "Alerts are off. You will not be told before a product runs out." and the button "Turn on". The banner SHALL NOT be dismissible. It SHALL be checked again every time the product list becomes visible, and SHALL disappear as soon as alerts can be shown again. With no products the banner SHALL NOT be shown. The banner SHALL stay readable at the largest font size and in light and dark theme.

#### Scenario: Banner after denial
- **GIVEN** Android 13 or later, one product stored, and the permission denied
- **WHEN** the product list is shown
- **THEN** the banner "Alerts are off. You will not be told before a product runs out." with the button "Turn on" is shown above the products

#### Scenario: No products, no banner
- **GIVEN** notifications turned off for the app and no product stored
- **WHEN** the product list is shown
- **THEN** only the empty state is shown, without the banner

#### Scenario: Channel turned off on an older Android
- **GIVEN** Android 8, one product stored, and the "Depletion alerts" channel turned off in system settings
- **WHEN** the product list is shown
- **THEN** the banner is shown

#### Scenario: Turned back on in settings
- **GIVEN** the banner is shown
- **WHEN** the user turns notifications on in system settings and returns to the app
- **THEN** the banner is no longer shown

### Requirement: Turning alerts on from the banner
Tapping "Turn on" SHALL, when the notification permission is not granted, always ask for it (with the explanation of "Explanation before asking again" skipped, since the banner already explains it). When the request ends without the permission and Android would not explain again, the app SHALL open the system notification settings of the app. When only notifications of the app or the "Depletion alerts" channel are turned off, it SHALL open the system notification settings of the app, or the settings of the channel when only the channel is turned off.

#### Scenario: Asking from the banner
- **GIVEN** Android 13 or later and the banner shown
- **WHEN** the user taps "Turn on"
- **THEN** the system dialog asking to allow notifications appears

#### Scenario: Permission permanently denied
- **GIVEN** Android 13 or later, the banner shown, and Android no longer shows the permission dialog
- **WHEN** the user taps "Turn on"
- **THEN** the request ends without a dialog and the system notification settings of the app are opened

#### Scenario: Denied for good from the banner
- **GIVEN** Android 13 or later and the banner shown
- **WHEN** the user taps "Turn on" and denies the system dialog so that Android will not explain again
- **THEN** the system notification settings of the app are opened

#### Scenario: Silent denial after saving
- **GIVEN** Android 13 or later and the permission denied for good
- **WHEN** the user saves a product
- **THEN** the request ends without a dialog, only the banner is shown, and no system settings are opened

#### Scenario: Only the channel is off
- **GIVEN** notifications allowed for the app, the "Depletion alerts" channel turned off, and the banner shown
- **WHEN** the user taps "Turn on"
- **THEN** the system settings of the "Depletion alerts" channel are opened

### Requirement: Alerts are kept while notifications are off
Turning notifications off or denying the permission SHALL NOT change which alerts are scheduled. An alert that becomes due while notifications are off SHALL be shown on the first reconcile after notifications are allowed again, as long as its depletion date has not passed.

#### Scenario: Allowed again before the depletion date
- **GIVEN** the permission denied and a product whose alert became due yesterday and whose depletion date is in 5 days
- **WHEN** the user allows notifications from the banner
- **THEN** the product's notification is shown without restarting the app
