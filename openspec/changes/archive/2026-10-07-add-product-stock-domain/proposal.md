## Why

Every Phase 1 feature (depletion date, alerts, persistence, screens) needs a product whose stock the app can estimate at any moment. That model does not exist yet: the domain module is empty. This change builds it first, as pure Kotlin, so later changes build on tested behavior instead of re-deriving it.

## What Changes

- Add the product model to the domain module: a product with a name, a unit label, a package size, a consumption rate and a stock.
- Validate every value at creation and on edit (non-blank name and unit label, positive package size, positive consumption rate, non-negative initial stock).
- Estimate the stock at any moment: it decreases continuously and linearly with elapsed time according to the consumption rate, is kept exact (fractional) internally, never goes below zero, and is exposed both exactly and as whole units (floored) with an indication of whether it was rounded.
- Support adjustments (+/- N units, clamped at zero), restocks (+k packages, k >= 1), and edits of the consumption rate and package size, with the rule for how each one affects the estimate.

## Capabilities

### New Capabilities
- `product-stock`: what a product is, which values are valid, and how its estimated stock evolves over time and through adjustments, restocks and edits.

### Modified Capabilities
- None.

## Depends on

None.

## Touches

`domain/` only (main and test sources). No changes to `:app`, the build configuration, CI or the gate.

## New dependencies

None. Time is handled with `java.time` from the JDK and exact arithmetic with `java.math` from the JDK.

## Non-goals

- Depletion date, lead time, rule, alert and next alert (change 3, `add-depletion-and-alert-domain`).
- Calendar days, time zones and daylight saving time: in this change a day is a fixed 24-hour elapsed duration; mapping to local dates belongs to change 3.
- Product identity and storage (persistence change).
- Any user interface, including how a rounded value is labeled on screen (UI changes).
- Pausing consumption (a rate of zero) and a closed list of units: out of the MVP.

## Impact

- New production code in the `:domain` module (estimated well under 400 lines) and its JVM unit tests.
- Establishes the domain types later changes will use; their names follow the glossary in `docs/SESSION0.md`.
