## Why

Products can now be stored (`add-room-persistence`), but the app still opens a placeholder screen. The user needs to see, at a glance, how much of each product is left, when it runs out and when its next alert fires (issue #42). This is the first real screen of Phase 1 and the place where the next changes (#43 create/edit/delete, #44 adjustment/restock) will plug in.

## What Changes

- Replace the placeholder screen with a product list screen: a top app bar with the app name and one row per stored product, in the order the repository returns them.
- Each row shows the product name, the estimated stock in whole units with the unit label as the user typed it, the depletion date and the next alert, using the owner-approved texts (see `specs/product-list/spec.md`).
- An empty state when no products are stored.
- The values are recomputed when the screen comes back to the foreground and every minute while it is visible; nothing runs in the background.
- A fixed Material 3 color scheme for the app, light or dark following the system setting, without dynamic color. The system bar icons follow the active scheme.
- The current time and time zone reach the UI through the `AppContainer`, so tests can control them.
- Remove the placeholder screen and its requirements in `project-structure` and `test-infrastructure`.

New dependencies (approving this proposal is the owner's approval of them; both use the existing `lifecycle` version in `gradle/libs.versions.toml`):
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `androidx.lifecycle:lifecycle-runtime-compose`

## Non-goals

- Adding, editing or deleting products, and any tap action on a row (#43).
- Adjustments and restocks (#44).
- Scheduling or delivering alerts, notifications or calendar events (#45, #46).
- Relative dates ("in 3 days"), sorting or filtering options, search.
- Brand colors and the app icon (#17); Spanish texts (#48).
- Background refresh or widgets.

## Capabilities

### New Capabilities
- `product-list`: the screen that lists the stored products with their estimated stock, depletion date and next alert, its empty state, its refresh behavior and its appearance (color scheme, dark mode, accessibility).

### Modified Capabilities
- `project-structure`: remove the "Placeholder main screen" requirement; the main screen is now the product list.
- `test-infrastructure`: remove the "Placeholder screen covered by a UI test" requirement; the product list has its own UI, screenshot and E2E coverage.

## Impact

- Depends on: #41 (`add-room-persistence`, merged).
- Touches: `:app` only (main activity, theme, product list UI and its ViewModel, `AppContainer`, string resources, JVM and instrumented tests, screenshot references in `app/src/test/screenshots/`), `gradle/libs.versions.toml` and `app/build.gradle.kts` for the two dependencies. `:domain` is not changed.
- Not parallelizable with #43 and #44, which build on this screen.
