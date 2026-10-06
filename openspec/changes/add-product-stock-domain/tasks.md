## 1. Value types

- [ ] 1.1 Add the exact quantity type (reduced `BigInteger` fraction) with the operations the estimate needs (subtract, add, multiply by whole numbers, compare, floor, is-whole), with unit tests including 3/2 exactness
- [ ] 1.2 Add `ProductName` and `UnitLabel` (trimmed, non-blank), `PackageSize` (positive) and `ConsumptionRate` (positive units and days), with unit tests for the valid and invalid cases in the "Product definition" requirement

## 2. Stock estimation

- [ ] 2.1 Add `Stock` (exact units + recorded instant) and `EstimatedStock` (exact value, whole units floored, rounded flag), with tests for "Continuous estimated stock" and "Whole-unit view of the estimate" scenarios (exact 8.5, runs out at 0, clock earlier than recording)

## 3. Product

- [ ] 3.1 Add `Product` creation with validation (initial whole-unit stock >= 0) and `estimatedStockAt(now)`, with tests for the "Product definition" scenarios
- [ ] 3.2 Add adjustment (non-zero, clamped at 0, re-anchors) and restock (k >= 1 packages, re-anchors), with tests for the "Adjustment" and "Restock" scenarios
- [ ] 3.3 Add editing of consumption rate (re-anchors at the change moment), package size, name and unit label (no stock change), with tests for the "Editing the consumption rate" and "Editing the package size and labels" scenarios

## 4. Verification

- [ ] 4.1 Run `./gradlew lintAll test` with zero warnings, and confirm production code stays within about 400 lines
- [ ] 4.2 Mark every task done and keep `design.md` in sync with any deviation decided during implementation
