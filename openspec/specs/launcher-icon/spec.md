## Purpose

Gives the app its own recognizable launcher icon, so the user can find it among their apps and the icon follows the system's icon shape and themed icons setting.

## Requirements

### Requirement: The app has its own adaptive launcher icon
The app SHALL declare an adaptive launcher icon, as both its icon and its round icon, made of a pure black (`#000000`) background layer and a white foreground mark: a tilted ellipse ring (about 45 degrees) enclosing a four-pointed star with concave arms and a dense center. The whole mark SHALL lie inside the adaptive icon safe zone, so no launcher mask shape cuts it. The app SHALL NOT use the system's default application icon.

#### Scenario: The launcher shows the app's icon
- **GIVEN** the app is installed on a device running Android 8.0 (API 26) or later
- **WHEN** the user opens the launcher's app list
- **THEN** the app is shown with the white tilted ring and star on black, not with the system's generic app icon

#### Scenario: Every mask shape keeps the mark whole
- **GIVEN** a launcher that masks icons as a circle, a squircle or a rounded square
- **WHEN** it shows the app's icon
- **THEN** the ring and the star are fully visible and centered within the shape

#### Scenario: The mark reads at small sizes
- **GIVEN** the app's icon is shown at 48dp
- **WHEN** the user looks at it
- **THEN** the ring and the four arms of the star are distinguishable, with no hairline strokes

### Requirement: The icon follows themed icons
The adaptive icon SHALL provide a monochrome layer with the same mark, so that on Android 13 (API 33) and later the system can tint it when the user turns on themed icons.

#### Scenario: Themed icons are on
- **GIVEN** a device running Android 13 or later with themed icons turned on
- **WHEN** the user opens the launcher's app list
- **THEN** the app's icon shows the same ring and star, tinted with the system's theme colors

#### Scenario: Themed icons are off
- **GIVEN** a device running Android 13 or later with themed icons turned off
- **WHEN** the user opens the launcher's app list
- **THEN** the app's icon shows the white mark on black
