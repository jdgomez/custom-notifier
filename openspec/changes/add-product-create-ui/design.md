## Context

See proposal.md for motivation and `specs/` for the behavior. Current state:
- `MainActivity` shows `ProductListScreen` directly inside `CustomNotifierTheme`; `ProductListViewModel` follows the pattern in `openspec/changes/archive/*-add-product-list-ui/design.md` (immutable state, `StateFlow`, `viewModelFactory` reading the `AppContainer`).
- `:domain` has `Product.create(...)` taking validated value types (`ProductName`, `UnitLabel`, `PackageSize`, `ConsumptionRate`, `Rule(LeadTime)`) plus `initialUnits` and `now`, and computes the depletion date and next alert.
- `AppContainer` exposes `productRepository`, the `Clock` and the zone provider.

## Goals / Non-Goals

**Goals:**
- A navigation setup the next screens (edit, adjustments) reuse without rework.
- A form whose state and validation are plain Kotlin, unit-testable without Compose, and which the edit change can reuse for an existing product.

**Non-Goals:**
- Deep links, multiple back stacks, tablets' two-pane layouts.
- Moving the form's input limits into `:domain` (see Decisions).

## Decisions

### Navigation 3 (ADR 0016), back stack in memory
The back stack is a `mutableStateListOf` of navigation keys (`ProductList`, `NewProduct`; the edit change adds `EditProduct(id)`) held by an activity-scoped ViewModel and rendered by `NavDisplay` in `MainActivity`; back pops the top key, and back on the list leaves the app. It survives configuration changes but not process death: after the system ends the process, the app starts again on the list (owner decision 2026-10-07, to keep dependencies minimal; the input of a short form is the only thing lost). Keys are plain Kotlin types, no `@Serializable`, no `rememberNavBackStack`, no `kotlinx-serialization`. Per-entry ViewModels use `rememberViewModelStoreNavEntryDecorator` from `lifecycle-viewmodel-navigation3`, so the form's ViewModel is cleared when its entry is popped.
- Alternatives (recorded in the ADR): restoring the back stack after process death with `rememberNavBackStack` (needs the `kotlinx-serialization` plugin and runtime; revisit if testers or a larger user base ask for it), `navigation-compose` 2.x (mature, but typed route arguments need the same serialization plugin, and the back stack is owned by the library), and hand-written navigation (no dependency, but the predictive back animation is ours to maintain).
- ADR 0016 "Navigation with AndroidX Navigation 3" follows the format of `docs/adr/0015-manual-dependency-injection.md`, records the no-restoration decision and its revisit trigger, and is added to `docs/adr/README.md`.

### Form state and validation
`ProductFormViewModel` keeps the raw field texts in its own state (it survives configuration changes; no `SavedStateHandle`, since nothing is restored after process death) and exposes a `StateFlow<ProductFormState>`: field texts, errors (shown only after the first "Save" tap, cleared per field on edit), the preview, whether input differs from the initial values, and a one-shot "saved" signal that the screen turns into popping the back stack.
- Input filtering (digits only, length caps) happens when a field changes; validation turns texts into the domain value types and the `initialUnits` integer, and reports a typed error per field, which the composable maps to the string resource.
- The limits (1 to 100,000; 1 to 365; 0 to 365; 0 to 1,000,000; 60 and 20 characters) are input rules of this form, kept in one place in the form's validation. The domain keeps its own invariants (positive sizes and rates, lead time 0 to 365). Alternative: tighten the domain value types. Rejected for now: the domain would reject data that other entry points (adjustments, restocks summing beyond 1,000,000) may legitimately produce.
- Preview: when all fields are valid, the ViewModel builds a transient `Product` with `Product.create(..., now)` and maps it to the same row status type the list uses, recomputed on every edit (no ticker: the form is short-lived). The composable reuses the list's date and status texts joined by " · ".
- Saving: `Product.create(..., now = clock.instant())`, `productRepository.save`, then the "saved" signal. Errors from the repository are not expected locally; if one happens it propagates (crash) rather than being swallowed, consistent with the list.

### Screen
- `Scaffold` with a `TopAppBar` (navigation icon with "Navigate up" content description, title, "Save" `TextButton`), content in a vertically scrolling `Column` with `imePadding`, `OutlinedTextField`s with `supportingText` for help and errors, `KeyboardOptions` (`KeyboardType.Number` for numbers, `ImeAction.Next`, `ImeAction.Done` on the last field). The two inline lines (consumption, lead time) use a `FlowRow` so they wrap at large font sizes.
- Back handling: a `BackHandler` enabled only while input differs from the initial values shows the `AlertDialog`; otherwise back pops the entry normally (predictive back animation preserved).
- Product list: `Scaffold` gains an `ExtendedFloatingActionButton` ("+" icon, "Add product") and the `LazyColumn` gets bottom content padding so the last row clears the button.

### Tests and evidence
- ViewModel unit tests: defaults, filtering, every validation rule and message type, error clearing, preview (including the spec's date example with a fixed clock and zone), save stores the expected product, "differs from initial" logic.
- Robolectric UI tests for the spec scenarios that need the composables (field order, consumption line follows the unit, focus on the first invalid field, dialog flow, back without input).
- Roborazzi references: empty form, form with errors, filled form with preview, the discard dialog, dark, font scale 2.0, and the product list with the button (empty and scrolled to the end). The existing product list references change because of the button; review the diffs.
- E2E: create a product end to end (open form, fill, save, row visible), discard flow, and rotation keeping the input.

## Risks / Trade-offs

- [Unsaved form input is lost when the system ends the process in the background] → Accepted by the owner; rare, and the form is short. Revisit per ADR 0016.
- [Predictive back with an enabled `BackHandler` shows no preview animation while input is dirty] → Accepted: the dialog must intercept the gesture.
- [Size: navigation, form, validation and preview are close to 400 production lines] → Strings and the ADR are not production code; if the count still exceeds about 400, report it before trimming features.
