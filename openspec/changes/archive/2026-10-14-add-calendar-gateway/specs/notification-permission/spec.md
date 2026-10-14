## MODIFIED Requirements

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
