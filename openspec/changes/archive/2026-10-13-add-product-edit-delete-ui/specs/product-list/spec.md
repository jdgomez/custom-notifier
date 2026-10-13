## MODIFIED Requirements

### Requirement: Main screen lists the stored products
When the app is launched, it SHALL show the product list screen: a top app bar with the app name and one row per stored product, in the order the stored products are provided (name, case-insensitive, then identity). Tapping a row SHALL open the "Edit product" form for that product, and accessibility services SHALL announce the row's action as "Edit".

#### Scenario: Products are listed in stored order
- **GIVEN** stored products named "Vitamin D", "diapers" and "Detergent"
- **WHEN** the user launches the app
- **THEN** the top app bar shows the app name
- **AND** the rows show "Detergent", "diapers" and "Vitamin D", in that order

#### Scenario: A stored change appears without relaunching
- **GIVEN** the product list is visible
- **WHEN** a product is stored, changed or removed
- **THEN** the list shows the change without the user leaving the screen

#### Scenario: Tapping a row opens it for editing
- **GIVEN** a stored product named "Vitamin D"
- **WHEN** the user taps its row
- **THEN** the "Edit product" form for "Vitamin D" is shown
