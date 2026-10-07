## Purpose
Defines what a product is, which of its values are valid, and how its estimated stock evolves over time and through adjustments, restocks and edits, so the app always knows how many units should remain.

## Requirements

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

### Requirement: Continuous estimated stock
The estimated stock at a given moment SHALL be the stock last recorded minus the consumption since it was recorded, where consumption grows continuously and linearly with elapsed time: N units every M days means N / M units per day, and a day is 24 hours of elapsed time. The estimate SHALL be computed exactly, without floating-point rounding. It SHALL never be below zero. A moment earlier than the last recording SHALL be treated as no elapsed time.

#### Scenario: Fractional consumption is exact
- **GIVEN** a product with a stock of 10 units and a consumption rate of 3 units every 2 days
- **WHEN** the estimated stock is computed exactly 1 day later
- **THEN** it is exactly 8.5 units

#### Scenario: Stock runs out
- **GIVEN** a product with a stock of 2 units and a consumption rate of 1 unit every 1 day
- **WHEN** the estimated stock is computed 5 days later
- **THEN** it is exactly 0 units

#### Scenario: Clock earlier than the recording
- **GIVEN** a product whose stock of 10 units was recorded at a moment T
- **WHEN** the estimated stock is computed at a moment before T
- **THEN** it is exactly 10 units

### Requirement: Whole-unit view of the estimate
Besides the exact value, the estimated stock SHALL be available as whole units, rounded down, together with an indication of whether rounding happened (the exact value was not a whole number).

#### Scenario: Fractional estimate
- **GIVEN** an exact estimated stock of 8.5 units
- **WHEN** its whole-unit view is read
- **THEN** it is 8 units and is marked as rounded

#### Scenario: Whole estimate
- **GIVEN** an exact estimated stock of 8 units
- **WHEN** its whole-unit view is read
- **THEN** it is 8 units and is not marked as rounded

### Requirement: Adjustment
An adjustment SHALL change the stock by a non-zero whole number of units, positive or negative, applied to the estimated stock at the moment of the adjustment. If the result would be below zero, the stock SHALL become exactly zero. The result SHALL become the recorded stock as of that moment, and later estimates SHALL start from it.

#### Scenario: Correcting downwards
- **GIVEN** a product with an estimated stock of exactly 8.5 units now
- **WHEN** the user adjusts it by -3 units
- **THEN** the estimated stock now is exactly 5.5 units, and later estimates decrease from 5.5 at the product's rate

#### Scenario: Adjustment below zero
- **GIVEN** a product with an estimated stock of 2 units now
- **WHEN** the user adjusts it by -5 units
- **THEN** the estimated stock now is exactly 0 units

#### Scenario: Zero adjustment
- **GIVEN** a product
- **WHEN** an adjustment of 0 units is requested
- **THEN** it is rejected with an error and the product does not change

### Requirement: Recording moment never moves back
Any operation that records a new stock (adjustment, restock, rate change) at a moment earlier than the last recording SHALL treat that moment as no elapsed time and SHALL keep the last recording moment as the new one. The stock SHALL never be negative in any state.

#### Scenario: Adjustment at an earlier moment
- **GIVEN** a product with a stock of 10 units recorded at T and a consumption rate of 1 unit every 1 day
- **WHEN** the user adjusts it by -3 units at a moment before T
- **THEN** the stock recorded at T is exactly 7 units, and the estimated stock at T + 1 day is exactly 6 units

#### Scenario: Negative stock is unrepresentable
- **GIVEN** a stock quantity below zero
- **WHEN** a stock or a product holding it is built or copied
- **THEN** it is rejected with an error naming the stock

### Requirement: Restock
A restock SHALL add k packages to the stock, k being a whole number of at least 1, that is k times the package size in units, applied to the estimated stock at the moment of the restock. The result SHALL become the recorded stock as of that moment.

#### Scenario: Restocking two packages
- **GIVEN** a product with a package size of 90 and an estimated stock of exactly 4.5 units now
- **WHEN** the user restocks 2 packages
- **THEN** the estimated stock now is exactly 184.5 units

#### Scenario: Restocking zero packages
- **GIVEN** a product
- **WHEN** a restock of 0 packages is requested
- **THEN** it is rejected with an error and the product does not change

### Requirement: Editing the consumption rate
Changing the consumption rate SHALL NOT change the stock already consumed: consumption up to the moment of the change SHALL use the old rate, and consumption after it SHALL use the new rate.

#### Scenario: Rate change mid-way
- **GIVEN** a product with a stock of 10 units recorded at T and a consumption rate of 1 unit every 1 day
- **WHEN** the rate is changed to 2 units every 1 day at T + 2 days
- **THEN** the estimated stock at T + 3 days is exactly 6 units

### Requirement: Editing the package size and labels
Changing the package size, the name or the unit label SHALL NOT change the estimated stock. A new package size SHALL apply only to restocks made after the change.

#### Scenario: Package size change
- **GIVEN** a product with a package size of 90 and an estimated stock of 30 units now
- **WHEN** the package size is changed to 60
- **THEN** the estimated stock now is still 30 units, and a later restock of 1 package adds 60 units

### Requirement: Product identity
Every product SHALL have an identity, assigned when the product is created and unique among all products. No operation on a product (adjustment, restock, editing the rate, package size, labels or rule) SHALL change its identity. Two products created with identical values SHALL have different identities.

#### Scenario: Identity is kept through edits
- **GIVEN** a product with an identity
- **WHEN** it is adjusted, restocked, renamed and its rule is changed
- **THEN** the resulting product has the same identity

#### Scenario: Identical values, different products
- **GIVEN** two products created with the same name, unit label, package size, consumption rate, stock and rule
- **WHEN** their identities are compared
- **THEN** they are different
