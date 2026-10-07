## 1. Setup

- [x] 1.1 Add `androidx.lifecycle:lifecycle-viewmodel-compose` and `androidx.lifecycle:lifecycle-runtime-compose` to `gradle/libs.versions.toml` (existing `lifecycle` version) and `app/build.gradle.kts` (owner approved with this proposal; no other new dependency)
- [x] 1.2 Expose a `Clock` and a zone provider from `AppContainer`, overridable for tests

## 2. State

- [x] 2.1 Add `ProductListState` and `ProductRow` (with the `Upcoming` / `RanOut` status) mapped only from domain results
- [x] 2.2 Add `ProductListViewModel`: repository flow combined with a 60 s ticker, `stateIn(WhileSubscribed(0))`, factory reading the `AppContainer`
- [x] 2.3 ViewModel unit tests per design.md (fixed clock and zone, virtual time)

## 3. UI

- [x] 3.1 Add `CustomNotifierTheme` with explicit light and dark color schemes, no dynamic color; switch `MainActivity` to `SystemBarStyle.auto` with transparent scrims
- [x] 3.2 Add the product list screen (top app bar, rows, empty state) with all texts as string resources, locale-aware dates and numbers, merged row semantics and the "about" accessibility text
- [x] 3.3 Show the screen from `MainActivity`; remove the placeholder screen, its tests and its screenshot reference
- [x] 3.4 Robolectric UI tests for every `product-list` scenario that does not need a device
- [x] 3.5 Roborazzi references: empty, all row cases, dark, font scale 2.0 with a long name; review every PNG for visual defects

## 4. End to end

- [x] 4.1 E2E tests: empty state on a clean install, and seeded products shown in order with the expected texts
- [x] 4.2 Run the app on `cn-api37` (light, dark, largest font) and `cn-api26`, check the screen pixel by pixel against the references, and fix anything that looks off
- [x] 4.3 Update `AGENTS.md` project state; `./gradlew assembleDebug lintAll test` and `scripts/e2e.sh` pass with zero warnings
