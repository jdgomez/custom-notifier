## Context

See proposal.md for motivation and `specs/` for the behavior. Current state (from `add-product-create-ui`, ADR 0016):
- `navigation/AppNavigation.kt`: `Destination` (`ProductList`, `NewProduct`), an in-memory back stack in `NavigationViewModel`, `NavDisplay` with per-entry ViewModels.
- `productform/`: `ProductFormField` (input rules), `ProductFormState` (texts, errors, preview, `isDirty` against `INITIAL_TEXTS`), `ProductFormEvent` (`Saved`, `FocusField`), `ProductFormViewModel` (filtering, validation, preview with `Product.create`, save), `ProductFormScreen.kt` (295 lines).
- `:domain` already has `rename`, `relabelUnit`, `changePackageSize`, `changeConsumptionRate(rate, now)`, `changeRule`, and `ProductRepository.find` / `delete`.

## Goals / Non-Goals

**Goals:**
- One form, two modes, without duplicating the screen or the validation.
- Keep production code within about 400 lines; the form screen should not grow much beyond the delete button and dialog.

**Non-Goals:**
- Cleaning up alerts or calendar events on delete (they do not exist yet).

## Decisions

### Navigation key
Add `Destination.EditProduct(val id: ProductId)`. The list row's click pushes it. The entry creates the form ViewModel with the id (the factory receives it as a parameter; `viewModel(factory = ...)` inside the entry, scoped per entry by the existing decorator).

### Mode in the ViewModel, not a second ViewModel
`ProductFormViewModel` takes an optional `ProductId`:
- New mode (no id): unchanged behavior.
- Edit mode: on creation it loads the product with `repository.find(id)`; until loaded the state is `loading` (the screen shows the top app bar and nothing else, typically for a single frame); if `find` returns null it emits a `Closed` event and the screen pops. The loaded product is kept as `original`; the texts start from its values and `isDirty` compares against those texts instead of `INITIAL_TEXTS` (the initial texts become a field of the state).
- The visible fields are `ProductFormField.entries` minus `UnitsNow` in edit mode; validation and the "first invalid field" use the same visible list.
- Preview in edit mode: apply the edited values to `original` (see Saving) and map it to `RowStatus` at the current moment, like the new product preview.
- Saving in edit mode: start from `original` and apply only what changed: `rename`, `relabelUnit`, `changePackageSize`, `changeRule`, and `changeConsumptionRate(rate, now)` only when the rate differs, so an unchanged rate does not re-anchor the stock. Then `repository.save` and the `Saved` event.
- Deleting: `onDelete()` calls `repository.delete(id)` and emits `Closed`. The dialog's name is `original.name`.
- Alternative: a separate `EditProductViewModel` sharing a validation helper. Rejected: more code for the same rules, and two places to keep in sync.

### Screen
`ProductFormScreen` reads the mode from the state: title "New product" or "Edit product", the "Units you have now" field only in new mode, and in edit mode a `TextButton` "Delete product" with `colorScheme.error` content color after the last field, opening an `AlertDialog` (title "Delete <name>?", text, "Cancel" and "Delete" with the error color). The existing discard dialog and `BackHandler` work unchanged through `isDirty`.

### Product list row
`ProductRowItem` becomes clickable with `onClickLabel` "Edit" (role button), passing the id up through `ProductListRoute(onEditProduct = ...)`. The merged row semantics stay.

### E2E assertion
Replace the gate's `onAllNodes(hasText("pills", substring = true)).onFirst()` in `ProductFormTest.createdProductIsListed` with an assertion on the exact stock text of the created row ("30 pills").

### Tests and evidence
- ViewModel unit tests: edit-mode loading (found, missing), prefilled texts, `isDirty` against stored values, hidden stock field in validation, preview from current stock, save applying only changed values (unchanged rate does not re-anchor; changed rate keeps past consumption per the spec example), delete.
- Robolectric UI tests for the scenarios that need composables (row tap opens the form with the right title, no stock field, delete button only in edit mode, delete and cancel dialog flow, dialog uses the stored name).
- Roborazzi: edit form, delete dialog, edit form at font scale 2.0; any changed list references reviewed.
- E2E: create, edit (rename) and delete a product end to end; the tightened assertion.

## Risks / Trade-offs

- [The list row becomes clickable while #44 will add stock actions to it] → #44 places its actions so they do not conflict with the row tap; recorded there when proposed.
- [A product deleted from the edit form is gone with no undo] → Owner decision; the dialog states it.
- [Size] → Expected around 150 to 200 production lines; if it clearly exceeds about 400, stop and report before trimming any feature.
