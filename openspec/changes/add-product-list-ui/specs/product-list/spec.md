## Purpose

Shows the user every stored product with its estimated stock, depletion date and next alert, so they can see at a glance what is running low and when they will be alerted.

## ADDED Requirements

### Requirement: Main screen lists the stored products
When the app is launched, it SHALL show the product list screen: a top app bar with the app name and one row per stored product, in the order the stored products are provided (name, case-insensitive, then identity). A row SHALL have no tap action.

#### Scenario: Products are listed in stored order
- **GIVEN** stored products named "Vitamin D", "diapers" and "Detergent"
- **WHEN** the user launches the app
- **THEN** the top app bar shows the app name
- **AND** the rows show "Detergent", "diapers" and "Vitamin D", in that order

#### Scenario: A stored change appears without relaunching
- **GIVEN** the product list is visible
- **WHEN** a product is stored, changed or removed
- **THEN** the list shows the change without the user leaving the screen

### Requirement: Empty state
When no products are stored, the screen SHALL show the title "No products yet" and the help line "Products you add will appear here with their stock and next alert." instead of the list.

#### Scenario: No products stored
- **GIVEN** no stored products
- **WHEN** the user launches the app
- **THEN** the screen shows "No products yet" and the help line, and no rows

### Requirement: Estimated stock in whole units
Each row SHALL show the estimated stock at the current moment rounded down to whole units, followed by a space and the unit label exactly as the user typed it, with no pluralization (for example "8 pills"). The number SHALL use the device locale's digit grouping. When the exact estimate is not a whole number, the text SHALL start with a muted "≈ " prefix and SHALL be announced to accessibility services as "about" followed by the number and the label. A zero estimate SHALL be shown in bold primary color, without any error styling.

#### Scenario: Whole estimate
- **GIVEN** a product with unit label "pills" whose estimated stock is exactly 8 units
- **WHEN** the list is shown
- **THEN** its row shows "8 pills" with no prefix

#### Scenario: Rounded estimate
- **GIVEN** a product with unit label "pills" whose estimated stock is 8.5 units
- **WHEN** the list is shown
- **THEN** its row shows "≈ 8 pills" with the "≈ " prefix muted
- **AND** accessibility services announce "about 8 pills"

#### Scenario: Out of stock
- **GIVEN** a product with unit label "pills" whose estimated stock is 0 units
- **WHEN** the list is shown
- **THEN** its row shows "0 pills" in bold primary color and no error color or icon

#### Scenario: Large stock
- **GIVEN** a device with an English (United States) locale and a product with unit label "ml" whose estimated stock is exactly 12000 units
- **WHEN** the list is shown
- **THEN** its row shows "12,000 ml"

### Requirement: Depletion date and next alert
Each row SHALL show the product's depletion date and next alert, computed in the device's current time zone. Dates SHALL use the device locale's medium date format; no relative dates and no times are shown.
- When the next alert is scheduled, the row SHALL show "Runs out <depletion date>" and "Alert <alert date>".
- When the next alert is due, the row SHALL show "Runs out <depletion date>" and "Alert due", the latter with a soft highlight.
- When there is no next alert because the depletion date has passed, the row SHALL show only "Ran out <depletion date>", replacing both lines.

#### Scenario: Alert scheduled
- **GIVEN** a device with an English (United States) locale, now is February 20, 2026, and a product that runs out on March 11, 2026 with a lead time of 10 days
- **WHEN** the list is shown
- **THEN** its row shows "Runs out Mar 11, 2026" and "Alert Mar 1, 2026"

#### Scenario: Alert due
- **GIVEN** a device with an English (United States) locale, now is March 5, 2026, and a product that runs out on March 11, 2026 with a lead time of 10 days
- **WHEN** the list is shown
- **THEN** its row shows "Runs out Mar 11, 2026" and a highlighted "Alert due"

#### Scenario: Already ran out
- **GIVEN** a device with an English (United States) locale, now is March 12, 2026, and a product that ran out on March 11, 2026
- **WHEN** the list is shown
- **THEN** its row shows "Ran out Mar 11, 2026" and neither a "Runs out" nor an "Alert" line

#### Scenario: Time zone of the device
- **GIVEN** a product whose stock reaches zero at 23:30 UTC on March 11, 2026, and a device in a time zone 2 hours ahead of UTC
- **WHEN** the list is shown
- **THEN** its depletion date is shown as March 12, 2026

### Requirement: Values stay current while visible
The screen SHALL recompute every row from the current moment and time zone when it comes to the foreground and at least once every minute while it is visible. It SHALL do no work while it is not visible.

#### Scenario: Time passes while visible
- **GIVEN** the product list is visible and a product's estimated stock is about to drop below 8 whole units
- **WHEN** one minute passes
- **THEN** the row shows the new estimate without any user action

#### Scenario: Returning to the app
- **GIVEN** the app was in the background while time passed
- **WHEN** the user brings it back to the foreground
- **THEN** every row shows values computed for the current moment

### Requirement: App color scheme
The app SHALL use its own fixed color scheme, light or dark following the system dark theme setting, and SHALL NOT use the wallpaper-based dynamic colors. The status and navigation bar icons SHALL stay legible on the active scheme.

#### Scenario: System dark theme
- **GIVEN** the system dark theme is on
- **WHEN** the user launches the app
- **THEN** the screen uses the app's dark color scheme with light system bar icons

#### Scenario: Dynamic colors are ignored
- **GIVEN** a device on Android 12 or later with a colorful wallpaper
- **WHEN** the user launches the app
- **THEN** the screen colors are the app's fixed colors, the same as on any other wallpaper

### Requirement: Readable at large font sizes
Every text on the screen SHALL scale with the system font size, and no text SHALL be truncated or overlap at the largest system font size; product names and stock texts wrap instead.

#### Scenario: Largest font size
- **GIVEN** the system font size set to its largest value and a product with a long name
- **WHEN** the list is shown
- **THEN** every text of the row is fully readable, wrapping onto more lines as needed
