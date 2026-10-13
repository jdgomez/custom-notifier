## Context

`:domain` already implements both operations on `Product`: `adjust` (non-zero whole units, applied to the estimated stock at a moment, clamped at zero) and `restock` (k >= 1 packages times the package size), both re-anchoring the stock at that moment and keeping the depletion date and alert consistent (`product-stock`, `depletion-and-alert`). The edit form (`add-product-edit-delete-ui`) loads the stored product in `ProductFormViewModel`, shows a live preview from the estimated stock now, and applies only changed values on "Save" so an unchanged rate does not re-anchor. This change is UI only.

## Goals / Non-Goals

**Goals:**
- Record a restock or an adjustment in a few taps from the place the user already goes to for a product (the row tap).
- Show the resulting stock before confirming, so a typo is caught before it is stored.
- Keep stock updates independent from field edits, so neither can silently discard the other.

**Non-Goals:**
- Quick actions on the list rows, a counted-stock input, history or undo (see proposal Non-goals and Owner decisions).

## Decisions

1. **Place: a "Stock" section at the top of the edit form.** The row tap already opens the edit form, and the stock is the value the user checks most, so it goes first. Alternatives: buttons on each list row (fastest, but adds two targets per row, risks accidental taps and crowds the row at large font sizes); a separate stock screen (one more navigation level for a two-tap action).
2. **Dialogs, applied immediately.** Each action is a small Material 3 `AlertDialog` that stores on confirm, through a new `ProductFormViewModel` operation that reloads the stored product, calls `restock` or `adjust` with the clock's current moment, saves it and refreshes the loaded product used for the stock section, the preview and the "Save" diff. Tying the stock to "Save" was rejected: the user would lose a restock by backing out with "Discard".
3. **"Save" never writes the stock.** "Save" already applies only changed fields to the stored product it re-reads, so a stock update between opening the form and "Save" is preserved. A test covers: restock, then rename and "Save", the stock keeps the restock.
4. **Adjustment as a difference ("Add" / "Remove" + units), matching the glossary.** "Remove" is selected first because correcting a stock down (spillage, the estimate ran slow) is the expected common case; a restock covers most increases.
5. **"Stock after" uses the list's stock text rules** (round down, "≈ " prefix, locale grouping) through the existing formatter, so the same product never reads differently on two screens.
6. **Limits:** restock count 1 to 99 (a stepper, no keyboard); adjustment units 1 to 1,000,000, the same upper bound as "Units you have now".
7. **Concurrency guard:** the confirm button is ignored while a stock update or a save or delete is in progress, reusing the form's single finishing guard.

## Risks / Trade-offs

- [The dialog's "Stock after" is computed when the dialog opens; the stored result uses the moment of confirming] -> the difference is at most a fraction of a unit per minute and the list rounding hides it; accepted.
- [Large stock plus restock exceeding the number range] -> `Stock` uses exact arithmetic; the UI caps inputs, so no overflow.
- [Two ways to change the stock-related look of the form (preview and stock section)] -> both read the same loaded product, refreshed after each update.

## Owner decisions (UX approved 2026-10-13)

1. Adjustment input is a difference ("Add" / "Remove" + units), as in the glossary; no counted-stock input.
2. Stock actions live only in the edit form's "Stock" section; no quick actions on the list rows.
3. No confirmation message after a restock or an adjustment.
4. The adjust dialog starts with "Remove" selected.
5. A restock adds 1 to 99 packages at once.

## Testing

- ViewModel unit tests: restock and adjust store the expected stock at the clock's moment; clamping at zero; limits; missing product returns to the list; "Save" after a stock update keeps the updated stock; stock update does not mark the form as changed.
- Robolectric UI tests for every `stock-update` scenario that needs no device.
- Roborazzi references: edit form with the stock section (light, dark, largest font), restock dialog, adjust dialog with and without units (light, dark).
- E2E: restock a product and see the new stock in the list; remove units and see the new stock in the list.
