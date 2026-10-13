## 0. Approval

- [x] 0.1 (owner) Approve the UX and answer the open questions in design.md (approved 2026-10-13, see design.md Owner decisions)

## 1. State

- [ ] 1.1 Add restock and adjust operations to `ProductFormViewModel` and the dialog state to `ProductFormState` per design.md (immediate store, shared finishing guard, refresh of the loaded product, missing product)
- [ ] 1.2 ViewModel unit tests per design.md, including "Save" after a stock update keeping the stock

## 2. UI

- [ ] 2.1 Stock section at the top of the edit form, reusing the list's stock text formatter
- [ ] 2.2 Restock dialog (stepper 1 to 99) and adjust dialog ("Add" / "Remove", "Units" field), all texts as string resources
- [ ] 2.3 Robolectric UI tests for the `stock-update` scenarios that do not need a device
- [ ] 2.4 Roborazzi references per design.md; review every new or changed PNG for visual defects

## 3. End to end

- [ ] 3.1 E2E tests: restock a product and remove units, checking the exact stock text in the list
- [ ] 3.2 Run the app on `cn-api37` (light, dark, largest font) and `cn-api26`, check the screens pixel by pixel against the references, and fix anything that looks off
- [ ] 3.3 Update `AGENTS.md` project state; `./gradlew assembleDebug lintAll test` and `scripts/e2e.sh` pass with zero warnings; report the production line count
