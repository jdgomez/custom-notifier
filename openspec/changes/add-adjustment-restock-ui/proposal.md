## Why

The user can create, edit and delete products, but cannot tell the app that they bought more or that the real stock differs from the estimate (issue #44). Without restocks and adjustments the estimated stock only goes down, so the depletion date and the alert drift away from reality after the first purchase.

## What Changes

- The "Edit product" form shows a "Stock" section above the fields: the estimated stock now (same text as the product list) and two buttons, "Restock" and "Adjust".
- "Restock" opens a dialog to add a number of packages (1 by default, stepper from 1 to 99) and shows the stock it will result in.
- "Adjust" opens a dialog to add or remove a number of units (1 to 1,000,000) and shows the stock it will result in; removing more than the stock leaves it at zero.
- Confirming either dialog stores the new stock right away (independent of the form's "Save"); the stock line and the live preview update, and unsaved field edits are kept.
- Both operations already exist in `:domain` (`Product.adjust`, `Product.restock`); this change adds only the UI.

No new dependencies.

## Non-goals

- Stock actions on the product list rows (a row tap keeps opening the edit form).
- Entering the counted stock directly instead of a +/- difference (owner decision, design.md).
- A history of adjustments and restocks, and undo.
- Restocks with a package size other than the product's.
- Rescheduling notifications or calendar events: they do not exist yet (#45, #46).

## Capabilities

### New Capabilities
- `stock-update`: the stock section of the edit form and the restock and adjust dialogs.

### Modified Capabilities
- None. The edit form's other requirements (`product-form`) are unchanged.

## Impact

- Depends on: #59 (`add-product-edit-delete-ui`, merged) and its archive (`product-form` main spec with the edit form).
- Touches: `:app` only (product form ViewModel, state and screen, new dialog composables, string resources, JVM and instrumented tests, screenshot references). `:domain` is unchanged.
- Parallelizable with changes that do not touch the product form (for example #17 icon); not with #45 or #46 if they change the form.
