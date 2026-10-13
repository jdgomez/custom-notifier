## Purpose

Lets the user record a restock (whole packages bought) or an adjustment (units added or removed to match reality) on a stored product, so the estimated stock, depletion date and alert follow what is really at home.

## ADDED Requirements

### Requirement: Stock section in the edit form
The "Edit product" form SHALL show, above its fields, a section with the label "Stock", the product's estimated stock at the current moment with the same text, rounding, "≈ " prefix and accessibility announcement as the product list row, and two buttons, "Restock" and "Adjust". The "New product" form SHALL NOT show this section.

#### Scenario: Stock shown in the edit form
- **GIVEN** a stored product with unit label "pills" whose estimated stock is 29.5 units
- **WHEN** the user opens its "Edit product" form
- **THEN** the form shows "Stock" with "≈ 29 pills" and the buttons "Restock" and "Adjust"

#### Scenario: No stock section for a new product
- **GIVEN** the product list is visible
- **WHEN** the user opens the "New product" form
- **THEN** no "Stock" section, "Restock" or "Adjust" button is shown

### Requirement: Restock dialog
Tapping "Restock" SHALL show the dialog "Restock <stored name>" with a package count starting at 1, a "-" and a "+" button that change it by 1 within 1 to 99 (each button disabled at its limit), the line "+<count × package size> <unit label>" and the line "Stock after: <stock>", where the stock after is the estimated stock now plus the added units, shown with the list's stock text rules. The buttons are "Cancel" (closes the dialog, nothing changes) and "Restock" (applies the restock).

#### Scenario: Restock preview
- **GIVEN** a product with package size 30, unit label "pills" and an estimated stock of exactly 5 units, with its "Edit product" form open
- **WHEN** the user taps "Restock" and then "+" once
- **THEN** the dialog shows the count 2, "+60 pills" and "Stock after: 65 pills"

#### Scenario: Count limits
- **GIVEN** the restock dialog with the count 1
- **WHEN** it is shown
- **THEN** "-" is disabled and "+" is enabled

#### Scenario: Cancel a restock
- **GIVEN** the restock dialog with the count 3
- **WHEN** the user taps "Cancel"
- **THEN** the dialog closes and the stock is unchanged

### Requirement: Adjust dialog
Tapping "Adjust" SHALL show the dialog "Adjust stock" with a choice between "Add" and "Remove" ("Remove" selected first), a number field "Units" that opens a numeric keyboard, accepts only digits and starts empty and focused, and the line "Stock after: <stock>", computed from the estimated stock now with the list's stock text rules and never below zero. While "Units" is empty or outside 1 to 1,000,000, the line SHALL be hidden and the "Adjust" button disabled. The buttons are "Cancel" (closes the dialog, nothing changes) and "Adjust" (applies the adjustment).

#### Scenario: Remove units
- **GIVEN** a product with unit label "pills" and an estimated stock of exactly 20 units, with the adjust dialog open
- **WHEN** the user types 3 in "Units"
- **THEN** the dialog shows "Stock after: 17 pills" and "Adjust" is enabled

#### Scenario: Remove more than the stock
- **GIVEN** a product with unit label "pills" and an estimated stock of exactly 2 units, with the adjust dialog open
- **WHEN** the user types 5 in "Units" with "Remove" selected
- **THEN** the dialog shows "Stock after: 0 pills"

#### Scenario: Add units
- **GIVEN** a product with unit label "pills" and an estimated stock of exactly 20 units, with the adjust dialog open
- **WHEN** the user selects "Add" and types 4 in "Units"
- **THEN** the dialog shows "Stock after: 24 pills"

#### Scenario: Empty units
- **GIVEN** the adjust dialog open
- **WHEN** "Units" is empty
- **THEN** no "Stock after" line is shown and "Adjust" is disabled

### Requirement: Applying a stock update
Confirming a restock or an adjustment SHALL store the product's new stock as of the current moment, as defined by the domain's Restock and Adjustment requirements, close the dialog and keep the user in the edit form. The stock section and the live preview SHALL show the new values. Unsaved edits in the form's fields SHALL be kept and SHALL still need "Save"; the stock update SHALL NOT count as an unsaved change. No confirmation message is shown. If the product is no longer stored, the app SHALL return to the product list.

#### Scenario: Restock is stored
- **GIVEN** a product with package size 30, unit label "pills" and an estimated stock of exactly 5 units, with the restock dialog open at count 1
- **WHEN** the user taps "Restock"
- **THEN** the dialog closes, the form shows "35 pills" in the stock section, and going back shows "35 pills" in the product's list row without the "Discard changes?" dialog

#### Scenario: Unsaved edits survive a stock update
- **GIVEN** the "Edit product" form with "Name" changed but not saved
- **WHEN** the user applies an adjustment
- **THEN** "Name" still shows the changed value
- **AND** going back shows the "Discard changes?" dialog

#### Scenario: Depletion moves with the stock
- **GIVEN** a product consuming 1 pill every 1 day with an estimated stock of exactly 5 pills, with its "Edit product" form open
- **WHEN** the user restocks 1 package of 30 pills
- **THEN** the live preview shows a depletion date 30 days later than before

### Requirement: Stock dialogs readable at large font sizes
Every text of the stock section and both dialogs SHALL scale with the system font size and stay fully readable at the largest size, wrapping instead of being truncated.

#### Scenario: Largest font
- **GIVEN** the system font size at its largest
- **WHEN** the restock dialog is shown for a product with a 60-character name
- **THEN** the title wraps and every text and button is fully visible
