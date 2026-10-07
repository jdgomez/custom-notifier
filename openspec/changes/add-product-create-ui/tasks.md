## 1. Navigation

- [x] 1.1 Add `navigation3-runtime`, `navigation3-ui` (new `navigation3` version, latest stable 1.x) and `lifecycle-viewmodel-navigation3` (existing `lifecycle` version) to `gradle/libs.versions.toml` and `app/build.gradle.kts` (owner approved with this proposal; no other new dependency or plugin)
- [x] 1.2 Add the navigation keys, the in-memory back stack in an activity-scoped ViewModel and `NavDisplay` in `MainActivity` with per-entry ViewModels; the product list is the start entry; no `kotlinx-serialization`
- [x] 1.3 Write ADR 0016 "Navigation with AndroidX Navigation 3" and add it to `docs/adr/README.md`

## 2. Form state

- [x] 2.1 Add `ProductFormViewModel` and `ProductFormState`: field texts, input filtering, validation to domain types with typed errors, preview, "differs from initial", save
- [x] 2.2 ViewModel unit tests per design.md

## 3. UI

- [x] 3.1 Add the "Add product" extended FAB and bottom content padding to the product list
- [x] 3.2 Add the "New product" form screen with all texts as string resources, keyboard options, scrolling with `imePadding`, wrapping inline lines, the preview and the discard dialog
- [x] 3.3 Robolectric UI tests for the `product-form` and `product-list` scenarios that do not need a device
- [x] 3.4 Roborazzi references per design.md; re-record the changed product list references and review every PNG for visual defects

## 4. End to end

- [x] 4.1 E2E tests: create a product and see it listed, discard flow, rotation keeps the input, and after the process is ended in the background the app reopens on the list
- [x] 4.2 Run the app on `cn-api37` (light, dark, largest font, predictive back) and `cn-api26`, check the screens pixel by pixel against the references, and fix anything that looks off
- [x] 4.3 Update `AGENTS.md` project state; `./gradlew assembleDebug lintAll test` and `scripts/e2e.sh` pass with zero warnings
