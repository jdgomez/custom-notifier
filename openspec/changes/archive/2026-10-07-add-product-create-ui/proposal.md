## Why

The app lists products (#42) but the user cannot add any yet. Creating a product is the first step of every use of the app (issue #43). The owner approved the UX of creating, editing and deleting products on 2026-10-07 and asked to split it in two changes because together they exceed the size limit: this change adds navigation and creating; `add-product-edit-delete-ui` (follow-up issue) adds editing and deleting on the same form.

## What Changes

- Navigation between screens, with the system back gesture (including predictive back) and state restored after process death. The product list is the start screen.
- An "Add product" floating action button on the product list that opens the product form.
- The product form for a new product: name, unit, units per package, consumption rate, lead time and the units the user has now, with the owner-approved texts, defaults, limits and validation messages (see `specs/product-form/spec.md`).
- A live preview of the depletion date and next alert while the form is filled in.
- Saving stores the product and returns to the list; leaving with unsaved input asks to discard it.
- ADR 0016: navigation with AndroidX Navigation 3.

New dependencies (approving this proposal is the owner's approval of them):
- `androidx.navigation3:navigation3-runtime` and `androidx.navigation3:navigation3-ui` (latest stable 1.x at implementation time, one new `navigation3` version in `gradle/libs.versions.toml`).
- `androidx.lifecycle:lifecycle-viewmodel-navigation3` (existing `lifecycle` version).

## Non-goals

- Editing and deleting products, and any tap action on a list row (`add-product-edit-delete-ui`).
- Adjustments and restocks (#44).
- Scheduling alerts or calendar events (#45, #46).
- Duplicate product detection (out of scope in `docs/SESSION0.md`).
- Decimal quantities or rates; consumption rate learning.

## Capabilities

### New Capabilities
- `product-form`: the screen to enter a product's data, its fields, defaults, limits, validation, preview, saving and discarding. This change covers creating a new product; editing and deleting extend it later.

### Modified Capabilities
- `product-list`: adds the "Add product" entry point to the list screen.

## Impact

- Depends on: #42 (`add-product-list-ui`, merged).
- Touches: `:app` only (navigation host in the main activity, product form screen and its ViewModel, product list screen for the button, string resources, JVM and instrumented tests, screenshot references), `gradle/libs.versions.toml`, `app/build.gradle.kts`, `docs/adr/` (new ADR and index). `:domain` is not changed.
- Not parallelizable with `add-product-edit-delete-ui`, which builds on this form.
