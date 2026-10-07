## ADDED Requirements

### Requirement: Add product entry point
The product list screen SHALL show, in both its empty and non-empty states, a floating action button at the bottom end of the screen with a "+" icon and the label "Add product", which opens the product form for a new product. The last row SHALL remain fully visible above the button when the list is scrolled to the end.

#### Scenario: Button on the empty list
- **GIVEN** no stored products
- **WHEN** the user launches the app
- **THEN** "No products yet" and the "Add product" button are shown

#### Scenario: Button opens the form
- **GIVEN** the product list is visible
- **WHEN** the user taps "Add product"
- **THEN** the "New product" form is shown

#### Scenario: Last row not hidden
- **GIVEN** more stored products than fit on the screen
- **WHEN** the user scrolls to the end of the list
- **THEN** the last row is fully visible above the "Add product" button
