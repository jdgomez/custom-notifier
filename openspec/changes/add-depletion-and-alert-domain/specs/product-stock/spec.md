## MODIFIED Requirements

### Requirement: Product definition
A product SHALL have a name, a unit label, a package size, a consumption rate, a stock and a rule. The name and the unit label SHALL be free text, stored trimmed of leading and trailing whitespace, and SHALL NOT be blank. The package size SHALL be a positive whole number of units. The consumption rate SHALL be N units every M days, with N and M positive whole numbers. The initial stock SHALL be a whole number of units, zero or greater. The rule SHALL be valid as defined by the `depletion-and-alert` capability. An invalid value SHALL be rejected with an error that names the invalid field, and no product SHALL be created or changed.

#### Scenario: Valid product
- **GIVEN** the name " Vitamin D ", the unit label "pill", a package size of 90, a consumption rate of 1 unit every 1 day, an initial stock of 30 and a rule with a lead time of 10 days
- **WHEN** the product is created
- **THEN** the product exists with the name "Vitamin D", the unit label "pill", a lead time of 10 days, and an estimated stock of exactly 30 units at the moment of creation

#### Scenario: Blank name or unit label
- **GIVEN** a name or a unit label that is empty or only whitespace
- **WHEN** a product is created or edited with it
- **THEN** it is rejected with an error naming that field

#### Scenario: Non-positive package size or rate
- **GIVEN** a package size of 0, or a consumption rate with 0 or negative units or days
- **WHEN** a product is created or edited with it
- **THEN** it is rejected with an error naming that field

#### Scenario: Negative initial stock
- **GIVEN** an initial stock of -1
- **WHEN** a product is created
- **THEN** it is rejected with an error naming the stock

#### Scenario: Invalid rule
- **GIVEN** a rule with a lead time of -1 days
- **WHEN** a product is created with it
- **THEN** it is rejected with an error naming the lead time
