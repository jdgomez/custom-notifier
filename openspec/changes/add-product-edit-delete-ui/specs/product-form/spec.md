## ADDED Requirements

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
