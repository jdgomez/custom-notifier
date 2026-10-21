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

### Requirement: Edit product form
The product form for a stored product SHALL have the title "Edit product" and the same fields, order, input rules, limits and validation messages as the new product form, except that "Units you have now" SHALL NOT be shown: the stock is not editable here. The fields SHALL start with the stored values. If the product is no longer stored when the form opens, the app SHALL return to the product list.

#### Scenario: Prefilled fields
- **GIVEN** a stored product "Vitamin D" with unit "pills", 90 units per package, 1 pills every 1 days and lead time 7
- **WHEN** the user opens it for editing
- **THEN** the "Edit product" form shows those values and no "Units you have now" field

#### Scenario: Same validation as a new product
- **GIVEN** the "Edit product" form
- **WHEN** the user clears "Name" and taps "Save"
- **THEN** "Enter a name" is shown below "Name", "Name" has focus and the stored product is unchanged

### Requirement: Saving changes
When the user taps "Save" and every field is valid, the app SHALL apply the edited values to the stored product, keep its estimated stock, and return to the product list. A changed consumption rate SHALL apply from the current moment: the consumption up to now keeps the previous rate. No confirmation message is shown.

#### Scenario: Rename
- **GIVEN** the "Edit product" form of "Vitamin D"
- **WHEN** the user changes "Name" to "Vitamin D3" and taps "Save"
- **THEN** the product list shows "Vitamin D3" with the same stock and no "Vitamin D"

#### Scenario: Rate change keeps past consumption
- **GIVEN** a product stored with 30 units at 1 unit every 1 day, 10 days ago
- **WHEN** the user changes the rate to 2 units every 1 day and saves
- **THEN** its estimated stock right after saving is 20 units
- **AND** the depletion date is 10 days from now

### Requirement: Edit preview
The edit form SHALL show the live preview with the same texts and rules as the new product form, computed from the product's estimated stock at the current moment and the edited values.

#### Scenario: Preview reflects a new lead time
- **GIVEN** a device with an English (United States) locale, now is February 20, 2026 at 10:00, and a stored product with 20 units left at 1 unit every 1 day and lead time 10
- **WHEN** the user changes the lead time to 5 in the edit form
- **THEN** the preview reads "Runs out Mar 12, 2026 · Alert Mar 7, 2026"

### Requirement: Leaving the edit form without saving
Going back from the edit form SHALL return to the product list directly when the fields still hold the stored values, and SHALL show the "Discard changes?" dialog, with the same buttons and behavior as for a new product, when any field differs from them.

#### Scenario: Back with changes
- **GIVEN** the "Edit product" form of "Vitamin D" with "Name" changed
- **WHEN** the user goes back and taps "Discard"
- **THEN** the product list shows "Vitamin D" unchanged

#### Scenario: Back without changes
- **GIVEN** the "Edit product" form with the stored values
- **WHEN** the user goes back
- **THEN** the product list is shown with no dialog

### Requirement: Deleting a product
The edit form SHALL end with a "Delete product" text button in the error color. Tapping it SHALL show the dialog "Delete <stored name>?" with the text "Its stock and alert will be removed. This can't be undone." and the buttons "Cancel" (closes the dialog, nothing changes) and "Delete" (removes the product and returns to the product list). There is no undo. The new product form SHALL NOT show the button.

#### Scenario: Delete a product
- **GIVEN** the "Edit product" form of "Vitamin D"
- **WHEN** the user taps "Delete product" and then "Delete"
- **THEN** the product list is shown without "Vitamin D"

#### Scenario: Cancel deleting
- **GIVEN** the dialog "Delete Vitamin D?" is shown
- **WHEN** the user taps "Cancel"
- **THEN** the "Edit product" form is shown and the product is still stored

#### Scenario: Dialog uses the stored name
- **GIVEN** the "Edit product" form of "Vitamin D" with "Name" changed to "Vitamin D3" and not saved
- **WHEN** the user taps "Delete product"
- **THEN** the dialog reads "Delete Vitamin D?"

#### Scenario: No delete button for a new product
- **GIVEN** the "New product" form
- **WHEN** the user scrolls to the end
- **THEN** no "Delete product" button is shown

### Requirement: Top bar readable at large font sizes
The product form's top bar SHALL show its whole title at every system font size and screen width without truncation. When the title fits on one line next to the back arrow and the save action, the bar SHALL be the usual one-line bar; only when it does not fit (measured, not guessed from the font size) SHALL the bar grow to show the title in full below the back arrow and the save action, which stay fully visible in their usual positions.

#### Scenario: Spanish edit title at the largest font size
- **GIVEN** the app in Spanish and the system font size set to its largest value
- **WHEN** the user opens a product to edit it
- **THEN** "Editar producto" is fully readable inside the top bar, not overlapping the content, and "Guardar" and the back arrow are fully visible on the same bar

#### Scenario: Default font size
- **GIVEN** the default system font size, in English or Spanish
- **WHEN** the "New product" or "Edit product" form is shown
- **THEN** the title fits on one line and the bar looks exactly as before this change (unchanged screenshot references)

### Requirement: Inline rows wrap by pairs
Every inline row of the product form that mixes number fields and words SHALL keep each number field together with the word that follows it; when the row does not fit on one line, it SHALL wrap only between these pairs, so no word is left alone on a line.

#### Scenario: Rule row at the largest font size
- **GIVEN** the system font size set to its largest value, in English or Spanish
- **WHEN** the product form is shown with a rule of 2 units every 1 day
- **THEN** the rule row shows "[2] cans every" and "[1] days" (or the Spanish equivalent) as two whole pairs, and no word stands alone on a line

#### Scenario: Rule row at the default font size
- **GIVEN** the default system font size
- **WHEN** the product form is shown
- **THEN** the rule row and the alert row each fit on one line, as before this change
