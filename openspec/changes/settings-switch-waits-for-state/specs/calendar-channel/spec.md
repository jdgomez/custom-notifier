## ADDED Requirements

### Requirement: Switch waits for the stored setting
Until the stored calendar channel setting has been read, the Settings screen SHALL show the switch "Add alerts to my calendar" disabled, in the same place and size as when it is enabled, with no progress indicator. A tap on it SHALL do nothing. Once the setting has been read, the switch SHALL show the real value and be interactive. No Settings control SHALL act on a value that has not been read yet.

#### Scenario: Setting not loaded yet
- **GIVEN** the Settings screen opened and the stored setting not yet read
- **WHEN** the user taps "Add alerts to my calendar"
- **THEN** nothing happens: no dialog opens, no permission is asked, and the setting is unchanged

#### Scenario: Setting loaded
- **GIVEN** the Settings screen opened and the stored setting read as on
- **WHEN** the setting has loaded
- **THEN** the switch is enabled and on, and tapping it turns the channel off
