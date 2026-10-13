## Why

The user can create products (#43) but cannot correct a typo, change a consumption rate that turned out wrong, or remove a product they no longer buy (issue #59). This is the second half of the owner-approved UX of 2026-10-07 for creating, editing and deleting products.

## What Changes

- Tapping a product row opens the product form for that product, titled "Edit product", with every field except the stock, prefilled with the stored values.
- Saving applies the changes to the stored product and returns to the list; a changed consumption rate keeps the consumption up to now at the old rate (domain behavior).
- The live preview shows the effect of the edited values from the product's current estimated stock.
- A "Delete product" button at the end of the edit form, with a confirmation dialog, removes the product and returns to the list.
- Leaving with unsaved changes asks to discard them, as for a new product.
- Tighten the E2E assertion the gate loosened in PR #60 ("pills" matched on the first of several nodes) to check the exact stock text.

No new dependencies.

## Non-goals

- Editing the stock: adjustments and restocks are #44.
- Undo after deleting (owner decision: no undo).
- Removing scheduled alerts or calendar events on delete: they do not exist yet; #45 and #46 must handle deletion when they add them.
- Restoring the form after process death (ADR 0016).

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `product-form`: adds the edit mode (prefilled fields without the stock, saving changes, preview from the current stock, leaving without saving) and deleting a product.
- `product-list`: a row opens its product for editing instead of having no tap action.

## Impact

- Depends on: #43 (`add-product-create-ui`, merged) and its archive (the `product-form` main spec must exist on `main` before this change is archived).
- Touches: `:app` only (navigation key for editing, product form ViewModel and screen, product list row, string resources, JVM and instrumented tests, screenshot references).
- Not parallelizable with #44, which also changes the list rows.
