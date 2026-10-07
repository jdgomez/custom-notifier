## Purpose
Keeps the user's products on the device so they survive the app being closed or the device restarting, and reads them back exactly as they were saved, with no loss of precision.

## Requirements

### Requirement: Saving and reading back products
The system SHALL store products on the device, in the app's private storage, and SHALL read each one back with exactly the values it was saved with: identity, name, unit label, package size, consumption rate, the exact stock (including fractional values), the moment the stock was recorded (to the nanosecond), and the rule with its lead time. Stored values SHALL never be interpreted or executed.

#### Scenario: Round trip of every value
- **GIVEN** a product named "Vitamin D" with the unit label "pill", a package size of 90, a consumption rate of 3 units every 2 days, an exact stock of 17/2 units recorded at 2026-03-01 12:00:00.123456789 UTC and a lead time of 10 days
- **WHEN** it is saved and then read back by its identity
- **THEN** the product read back is equal to the one saved, value by value

#### Scenario: Text is stored literally
- **GIVEN** a product named `Robert'); DROP TABLE products;--`
- **WHEN** it is saved and read back
- **THEN** its name is exactly that text and every other stored product is unaffected

#### Scenario: Products survive a restart
- **GIVEN** a saved product
- **WHEN** the app process ends and the products are read again from a new process
- **THEN** the product is there with the same values

### Requirement: Updating and deleting
Saving a product whose identity is already stored SHALL replace the stored product. Deleting a product by identity SHALL remove it; deleting an identity that is not stored SHALL have no effect and SHALL NOT fail. Finding an identity that is not stored SHALL return no product.

#### Scenario: Saving again replaces
- **GIVEN** a saved product with a stock of 30 units
- **WHEN** it is restocked by 1 package of 90 units and saved again
- **THEN** reading it back gives a stock of 120 units, and there is still only one product with that identity

#### Scenario: Delete
- **GIVEN** two saved products
- **WHEN** one of them is deleted
- **THEN** finding it returns no product and the other is still stored

#### Scenario: Unknown identity
- **GIVEN** an identity that was never saved or was deleted
- **WHEN** it is found or deleted
- **THEN** finding returns no product and deleting does nothing, without error

### Requirement: Observing all products
The system SHALL let callers observe the list of all stored products: observing SHALL first deliver the current list, then deliver the new list after every save or delete. The list SHALL be ordered by name, case-insensitively, and products with equal names by identity, so the order is stable.

#### Scenario: Updates are delivered
- **GIVEN** an observer of all products and no stored products
- **WHEN** a product is saved, then edited and saved again, then deleted
- **THEN** the observer receives, in order, an empty list, a list with the product, a list with the edited product, and an empty list

#### Scenario: Order by name
- **GIVEN** stored products named "diapers", "Vitamin D" and "Detergent"
- **WHEN** all products are observed
- **THEN** the list is "Detergent", "diapers", "Vitamin D"
