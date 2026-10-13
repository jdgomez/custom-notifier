## 1. State

- [x] 1.1 Add `Destination.EditProduct(id)` and push it from a row tap
- [x] 1.2 Extend `ProductFormViewModel` and `ProductFormState` with the edit mode per design.md: loading, missing product, initial texts from the stored values, visible fields without the stock, preview from the current stock, save applying only changed values, delete
- [x] 1.3 ViewModel unit tests per design.md

## 2. UI

- [x] 2.1 Make product list rows clickable with the "Edit" accessibility label
- [x] 2.2 Update the form screen for edit mode: title, no stock field, "Delete product" button and the delete dialog, all texts as string resources
- [x] 2.3 Robolectric UI tests for the `product-form` and `product-list` scenarios that do not need a device
- [x] 2.4 Roborazzi references per design.md; review every new or changed PNG for visual defects

## 3. End to end

- [x] 3.1 E2E tests: edit (rename) and delete a product; replace the loosened "pills" assertion in `ProductFormTest.createdProductIsListed` with the exact stock text
- [x] 3.2 Run the app on `cn-api37` (light, dark, largest font, predictive back) and `cn-api26`, check the screens pixel by pixel against the references, and fix anything that looks off
- [x] 3.3 Update `AGENTS.md` project state; `./gradlew assembleDebug lintAll test` and `scripts/e2e.sh` pass with zero warnings; report the production line count
