## Purpose
How the user turns the calendar alert channel on and off for the whole app, grants calendar access and chooses the calendar that receives alert events, from the Settings screen.

## ADDED Requirements

### Requirement: Settings screen
The product list top bar SHALL show a settings action (gear icon, accessibility label "Settings") that opens the Settings screen. The Settings screen SHALL have the title "Settings" and a back action (accessibility label "Back") that returns to the product list, as does the system back gesture. It SHALL show a section titled "Calendar" with a switch labelled "Add alerts to my calendar" and the supporting text "Each alert also appears as an all-day event in the calendar you choose." The screen SHALL stay readable at the largest font size and in light and dark theme.

#### Scenario: Opening settings
- **GIVEN** the product list
- **WHEN** the user taps the "Settings" action
- **THEN** the Settings screen is shown with the "Calendar" section and the switch "Add alerts to my calendar" off

#### Scenario: Going back
- **GIVEN** the Settings screen
- **WHEN** the user taps "Back" or uses the system back gesture
- **THEN** the product list is shown

### Requirement: Calendar channel setting
The calendar channel SHALL be a single setting for all products: off, or on with exactly one chosen calendar. It SHALL be off on a fresh install and SHALL be kept across app restarts and device reboots. It SHALL become on only when the user has granted calendar access and chosen a calendar. While it is on, the Settings screen SHALL show, below the switch, a row titled "Calendar" showing the chosen calendar's name and, below it, its account name; tapping the row SHALL open the calendar chooser to change it. Turning the switch off SHALL turn the channel off immediately, without confirmation, and hide the row.

#### Scenario: Channel kept after restart
- **GIVEN** the channel on with the calendar "Home" of the account "user@example.com"
- **WHEN** the app is closed and opened again and the user opens Settings
- **THEN** the switch is on and the "Calendar" row shows "Home" and "user@example.com"

#### Scenario: Turning the channel off
- **GIVEN** the channel on
- **WHEN** the user turns the switch off
- **THEN** the switch is off, the "Calendar" row is hidden, and after reopening Settings the switch is still off

#### Scenario: Changing the calendar
- **GIVEN** the channel on with the calendar "Home"
- **WHEN** the user taps the "Calendar" row and chooses "Family"
- **THEN** the row shows "Family" and the channel stays on

### Requirement: Asking for calendar access
When the user turns the switch on and the app does not have calendar access, the app SHALL ask for it (read and write calendar, one system dialog). The app SHALL NOT ask for calendar access at launch or at any other moment. When Android reports that the app should explain why it needs access, the app SHALL first show a dialog titled "Allow calendar access?" with the text "Custom Notifier uses calendar access only to list your calendars and to add one all-day event for each alert." and the buttons "Continue" (shows the system dialog) and "Not now" (closes it, switch stays off). When the user denies access, the switch SHALL stay off. When Android no longer shows the system dialog because the user denied it for good, turning the switch on SHALL show a dialog titled "Calendar access is off" with the text "To add alerts to your calendar, allow calendar access for Custom Notifier in the system settings." and the buttons "Open settings" (opens the app's system settings) and "Cancel"; the switch SHALL stay off. There SHALL be no other way to add alerts to a calendar without access. When access is granted, the calendar chooser SHALL open.

#### Scenario: First time on
- **GIVEN** calendar access never asked
- **WHEN** the user turns the switch on
- **THEN** the system dialog asking for calendar access appears

#### Scenario: Access granted
- **GIVEN** the system dialog for calendar access
- **WHEN** the user allows it
- **THEN** the calendar chooser opens

#### Scenario: Access denied
- **GIVEN** the system dialog for calendar access
- **WHEN** the user denies it
- **THEN** the dialog closes and the switch is off

#### Scenario: Asking after an earlier denial
- **GIVEN** the user denied calendar access once and Android reports that an explanation should be shown
- **WHEN** the user turns the switch on
- **THEN** the "Allow calendar access?" dialog is shown, and tapping "Continue" shows the system dialog

#### Scenario: Denied for good
- **GIVEN** the user denied calendar access and Android no longer shows the system dialog
- **WHEN** the user turns the switch on
- **THEN** the "Calendar access is off" dialog is shown, the switch stays off, and "Open settings" opens the app's system settings

#### Scenario: No asking at launch
- **GIVEN** calendar access not granted
- **WHEN** the user opens the app and the Settings screen
- **THEN** no calendar access dialog is shown

### Requirement: Calendar chooser
The calendar chooser SHALL be a dialog titled "Choose a calendar" listing every calendar on the device that is visible, synced and writable by the app, each with its name and, below it, its account name, with the current choice selected when there is one. Tapping a calendar SHALL choose it, turn the channel on with it and close the dialog. "Cancel" SHALL close the dialog without changes: when the channel was off it stays off. When there is no such calendar, the dialog SHALL instead be titled "No calendar available" with the text "There is no calendar on this device that Custom Notifier can add events to. Add an account in your calendar app, then try again." and the button "OK", and the channel SHALL stay as it was.

#### Scenario: Choosing a calendar
- **GIVEN** calendar access granted and the writable calendars "Home" and "Family"
- **WHEN** the user turns the switch on and taps "Home" in the chooser
- **THEN** the dialog closes, the switch is on and the "Calendar" row shows "Home"

#### Scenario: Read-only calendars are not offered
- **GIVEN** a writable calendar "Home" and a read-only calendar "Holidays"
- **WHEN** the chooser opens
- **THEN** it lists "Home" and does not list "Holidays"

#### Scenario: Cancelling the first choice
- **GIVEN** the channel off and the chooser open after granting access
- **WHEN** the user taps "Cancel"
- **THEN** the dialog closes and the switch is off

#### Scenario: No calendar on the device
- **GIVEN** calendar access granted and no writable calendar on the device
- **WHEN** the user turns the switch on
- **THEN** the "No calendar available" dialog is shown, and after "OK" the switch is off

### Requirement: Reporting a channel that cannot work
Every time the Settings screen becomes visible while the channel is on, the app SHALL check calendar access and the chosen calendar. When calendar access was removed, the screen SHALL show, below the switch, the text "Calendar access was removed. Alerts are not added to your calendar." with the button "Allow", which follows the same asking flow as turning the switch on (without opening the chooser when the chosen calendar is still usable). When the chosen calendar no longer exists or is no longer writable, the "Calendar" row SHALL show "The chosen calendar is no longer available. Choose another one." instead of the calendar's name, and tapping it SHALL open the chooser. In both cases the channel SHALL stay on with its stored choice until the user changes it or turns it off. The messages SHALL disappear as soon as the problem is fixed.

#### Scenario: Access removed
- **GIVEN** the channel on and calendar access revoked in the system settings
- **WHEN** the user opens Settings
- **THEN** the switch is on and the text "Calendar access was removed. Alerts are not added to your calendar." with the button "Allow" is shown

#### Scenario: Access given back
- **GIVEN** the "Calendar access was removed" text on the Settings screen
- **WHEN** the user taps "Allow" and allows calendar access
- **THEN** the text disappears and the "Calendar" row shows the chosen calendar

#### Scenario: Calendar removed
- **GIVEN** the channel on with the calendar "Home" and the calendar "Home" removed from the device
- **WHEN** the user opens Settings
- **THEN** the "Calendar" row shows "The chosen calendar is no longer available. Choose another one." and tapping it opens the chooser
