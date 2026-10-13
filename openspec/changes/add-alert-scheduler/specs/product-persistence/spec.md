## ADDED Requirements

### Requirement: Alert delivery records are stored
The app SHALL store, per product, whether its current alert has been delivered, so that the record survives app restarts, device reboots and app updates. Deleting a product SHALL delete its record. Upgrading from the schema without delivery records SHALL keep every stored product unchanged, with no alert recorded as delivered.

#### Scenario: Record survives a restart
- **GIVEN** a product whose alert was delivered
- **WHEN** the app process is killed and started again
- **THEN** the product's alert is still recorded as delivered

#### Scenario: Upgrade keeps the products
- **GIVEN** a database at the previous schema version with two stored products
- **WHEN** the app is updated to this version and started
- **THEN** both products are stored with the same values and neither alert is recorded as delivered
