## ADDED Requirements

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
