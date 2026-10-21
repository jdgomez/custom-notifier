## Why

At the largest system font size the product form breaks in two places, found while taking the store screenshots for #47 (owner decision 2026-10-21, options A): in Spanish the edit title "Editar producto" wraps to two lines inside a one-line top bar, overflows it and pushes "Guardar"; and in both languages the inline rule row ("2 cans every 1 days") breaks per element, leaving the last word alone on its own line. Both go against the form's readability at large font sizes.

## What Changes

- The product form's top bar grows with its title: a title that needs two lines is shown in full, and the back arrow and the save action stay in place and fully visible. Applies to the "New product" and "Edit product" forms.
- The inline rows of the form (the rule row and the "alert ... days before" row) wrap by pairs: each number field stays together with the word that follows it, so a wrapped row never leaves a word alone on a line.
- At the default font size both screens look as today (screenshot references change only where the large-font layout is covered).

No new dependencies (Material 3 top bars are already part of the Compose Material 3 library in use).

## Non-goals

- The product list: the "Add product" button covering the last row on the first screen at the largest font size is expected, since the list scrolls and its end stays clear of the button (owner decision 2026-10-21: no change).
- Shortening or truncating titles.
- Other screens (Settings, dialogs) unless the same defect shows up there during the check; then it is reported, not fixed in this change.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `product-form`: new requirements for the top bar at large font sizes and for the wrapping of inline rows.

## Impact

- Depends on: nothing. Independent of #47 (`add-play-internal-testing`), which retakes its screenshots at the default font size.
- Touches: `app/src/main/.../ProductFormScreen.kt`, its tests and Roborazzi references. Uses the emulator for the hand check: not in parallel with another flow's emulator use.
- Issue: none (found during #47); the pull request says "Refs #47".
