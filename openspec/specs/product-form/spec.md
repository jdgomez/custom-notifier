## Purpose

Lets the user enter a product's data (name, unit, package size, consumption rate, lead time and, for a new product, the units they have now) with immediate feedback on when it runs out and when they will be alerted.

## Requirements

### Requirement: New product form
The product form for a new product SHALL be a full screen with a top app bar holding a back arrow, the title "New product" and a "Save" action, followed by these fields in this order:
- "Name", a text field.
- "Unit", a text field with the supporting text "What you count, as it will be shown: pills, diapers, ml".
- "Units per package", a number field.
- The consumption rate as one line: a number field, the unit as typed in "Unit" (or "units" while "Unit" is blank), the word "every", a number field and the word "days", reading for example "3 pills every 2 days".
- The lead time as one line: "Alert", a number field and "days before it runs out", with the supporting text "0 alerts on the day it runs out".
- "Units you have now", a number field.

#### Scenario: Opening the form
- **GIVEN** the product list is visible
- **WHEN** the user taps "Add product"
- **THEN** the "New product" form is shown with the fields in the specified order

#### Scenario: The consumption line follows the unit
- **GIVEN** the "New product" form with "Unit" blank
- **WHEN** the user types "pills" in "Unit"
- **THEN** the consumption line changes from "units every" to "pills every"

### Requirement: Defaults and input rules
A new product form SHALL start with every field empty except the consumption days, set to 1, and the lead time, set to 7. Number fields SHALL open a numeric keyboard and SHALL accept only digits (no sign, no decimal separator). "Name" SHALL accept at most 60 characters and "Unit" at most 20. The keyboard's next action SHALL move to the following field, and the focused field SHALL stay visible above the keyboard.

#### Scenario: Defaults
- **GIVEN** the product list is visible
- **WHEN** the user opens the "New product" form
- **THEN** "every" shows 1, "Alert" shows 7 and every other field is empty

#### Scenario: Only digits in number fields
- **GIVEN** the "New product" form
- **WHEN** the user types "-2.5" in "Units per package"
- **THEN** the field shows "25"

#### Scenario: Name length limit
- **GIVEN** the "New product" form
- **WHEN** the user types a 61-character name
- **THEN** the field keeps only the first 60 characters

### Requirement: Validation on save
When the user taps "Save", every invalid field SHALL show its error message below it, the first invalid field SHALL receive focus, and nothing SHALL be stored. Names and units are compared after trimming spaces. Duplicate names SHALL be allowed. The rules and messages are:
- "Name" blank: "Enter a name".
- "Unit" blank: "Enter a unit".
- "Units per package" and the consumption units, empty or outside 1 to 100,000: "Enter a whole number from 1 to 100,000".
- The consumption days, empty or outside 1 to 365: "Enter a whole number from 1 to 365".
- The lead time, empty or outside 0 to 365: "Enter a whole number from 0 to 365".
- "Units you have now", empty or outside 0 to 1,000,000: "Enter a whole number from 0 to 1,000,000".
An error SHALL disappear as soon as its field is edited.

#### Scenario: Missing name and package size
- **GIVEN** the "New product" form with "Name" containing only spaces and "Units per package" empty
- **WHEN** the user taps "Save"
- **THEN** "Enter a name" is shown below "Name" and "Enter a whole number from 1 to 100,000" below "Units per package"
- **AND** "Name" has focus and no product is stored

#### Scenario: Out of range values
- **GIVEN** the "New product" form with consumption days 400 and lead time 366
- **WHEN** the user taps "Save"
- **THEN** "Enter a whole number from 1 to 365" is shown below the consumption days and "Enter a whole number from 0 to 365" below the lead time

#### Scenario: Error clears on edit
- **GIVEN** "Enter a name" is shown below "Name"
- **WHEN** the user types a character in "Name"
- **THEN** the error is no longer shown

#### Scenario: Duplicate name
- **GIVEN** a stored product named "Vitamin D"
- **WHEN** the user saves a valid new product also named "Vitamin D"
- **THEN** both products are stored and listed

### Requirement: Live preview
While every field holds a valid value, the form SHALL show below the fields a preview of how the product will appear in the list, computed from the current moment and the device time zone, using the product list's texts joined by " · ": "Runs out <date> · Alert <date>", "Runs out <date> · Alert due", or "Ran out <date>". While any field is empty or invalid, no preview SHALL be shown.

#### Scenario: Preview while typing
- **GIVEN** a device with an English (United States) locale, now is February 20, 2026 at 10:00, and the form filled in with 20 units now, 1 unit every 1 day and lead time 10
- **WHEN** the form is shown
- **THEN** the preview reads "Runs out Mar 12, 2026 · Alert Mar 2, 2026"

#### Scenario: Preview hidden while incomplete
- **GIVEN** the form with "Units you have now" empty
- **WHEN** the form is shown
- **THEN** no preview is shown

### Requirement: Saving a new product
When the user taps "Save" and every field is valid, the app SHALL store a new product with the entered values, its stock being the entered units as of the current moment, and SHALL return to the product list, which shows the new product. No confirmation message is shown.

#### Scenario: Save a product
- **GIVEN** the form filled in with name "Vitamin D", unit "pills", 90 units per package, 1 pills every 1 days, lead time 7 and 30 units now
- **WHEN** the user taps "Save"
- **THEN** the product list is shown and contains a row "Vitamin D" with "30 pills"

### Requirement: Leaving without saving
Going back from the form (the top app bar arrow or the system back gesture) SHALL return to the product list directly when the fields still hold their initial values. When any field differs from its initial value, it SHALL show the dialog "Discard changes?" with the buttons "Keep editing" (closes the dialog, input kept) and "Discard" (returns to the list, nothing stored). The entered input SHALL survive configuration changes (rotation, dark theme, font size, locale). When the system ends the app's process in the background, the app SHALL reopen on the product list and the unsaved input is lost (owner decision 2026-10-07: no restoration after process death).

#### Scenario: Back with input
- **GIVEN** the "New product" form with "Vitamin D" typed in "Name"
- **WHEN** the user goes back
- **THEN** the dialog "Discard changes?" is shown
- **AND** tapping "Discard" shows the product list with no new product

#### Scenario: Keep editing
- **GIVEN** the dialog "Discard changes?" is shown
- **WHEN** the user taps "Keep editing"
- **THEN** the form is shown with the input unchanged

#### Scenario: Back without input
- **GIVEN** the "New product" form with its initial values
- **WHEN** the user goes back
- **THEN** the product list is shown with no dialog

#### Scenario: Rotation keeps the input
- **GIVEN** the "New product" form with "Vitamin D" typed in "Name"
- **WHEN** the device is rotated
- **THEN** "Name" still contains "Vitamin D"

#### Scenario: Process ended in the background
- **GIVEN** the "New product" form with "Vitamin D" typed in "Name" and the app in the background
- **WHEN** the system ends the app's process and the user returns to the app
- **THEN** the product list is shown and no product is stored

### Requirement: Form readable at large font sizes
Every text of the form SHALL scale with the system font size and stay fully readable at the largest size, wrapping instead of being truncated; the form SHALL scroll when it does not fit.

#### Scenario: Largest font size
- **GIVEN** the system font size set to its largest value
- **WHEN** the "New product" form is shown
- **THEN** every label, supporting text and error is fully readable and the user can scroll to every field
