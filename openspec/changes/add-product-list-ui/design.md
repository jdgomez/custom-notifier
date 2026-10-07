## Context

See proposal.md for motivation and `specs/product-list/spec.md` for the behavior. Current state:
- `:domain` already computes everything the rows need: `Product.estimatedStockAt(now)` (exact and whole-unit view, `rounded` flag), `Product.depletionDateIn(zone)` and `Product.nextAlertAt(now, zone)` (`Scheduled` / `Due` / `None`).
- `ProductRepository.observeAll()` emits the products already ordered by name (case-insensitive) then identity, and re-emits after every change.
- `AppContainer` (manual DI, ADR 0015) owns the repository; `CustomNotifierApplication` owns the container.
- `MainActivity` shows a placeholder in a bare `MaterialTheme` and forces light system bar styles.

## Goals / Non-Goals

**Goals:**
- A ViewModel-backed screen whose state is a pure function of (products, now, zone), so it is easy to test with a fixed clock and zone.
- Domain logic stays in `:domain`; the ViewModel only maps domain results to UI state; composables only format and lay out.
- Set the pattern the next screens (#43, #44) will follow.

**Non-Goals:**
- Navigation. With one screen there is nothing to navigate; #43 introduces it when it adds a second screen.
- A brand palette (comes with the icon, #17).

## Decisions

### ViewModel + immutable UI state, collected with the lifecycle
`ProductListViewModel` exposes a `StateFlow<ProductListState>` (`Loading`, `Empty`, `Products(rows)`), collected in Compose with `collectAsStateWithLifecycle`. Each `ProductRow` holds already-computed values: id, name, `EstimatedStock` (whole units + rounded flag), unit label, and a row status that is either `Upcoming(depletionDate, nextAlert: Scheduled date | Due)` or `RanOut(depletionDate)`. The ViewModel is created with a `viewModelFactory` initializer that reads the `AppContainer`; no DI library (ADR 0015 still holds). Owner decided on 2026-10-07: no ADR for this pattern, it is standard Android practice and is recorded here.
- Alternative: state held directly in the Activity or a plain class. Rejected: it does not survive configuration changes and would not set a reusable pattern.
- Text formatting (dates, number grouping, "≈", accessibility text) happens in the composables with the current configuration locale, so a locale change re-formats without touching the ViewModel.

### Time source through the AppContainer
`AppContainer` exposes a `Clock` (for `Instant`s, `Clock.systemUTC()`) and a zone provider (`() -> ZoneId`, defaulting to `ZoneId::systemDefault`) that is read on every recompute, so a time zone change is picked up on the next tick or resume. Tests pass a fixed clock and zone.
- Alternative: `Clock.systemDefaultZone()` alone. Rejected: its zone is fixed when the clock is created.

### Refresh: ticker combined with the repository flow, only while observed
The state is `combine(repository.observeAll(), ticker)` mapped through the domain, exposed with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), Loading)`. The ticker emits once on subscription and then every 60 seconds. `collectAsStateWithLifecycle` subscribes at `STARTED` and unsubscribes at `STOPPED`, so returning to the foreground recomputes immediately and nothing runs in the background.
- Alternative: a stop timeout (for example 5 s) to keep the flow through rotations. Rejected: on a quick return it would show values up to a minute old; recomputing on rotation is cheap.
- Alternative: aligning ticks to wall-clock minutes. Not needed: dates change at most once a day and the stock text by whole units; a 60 s period meets "at least once every minute".

### Theme
A `CustomNotifierTheme` composable with explicit light and dark `ColorScheme`s (the Material 3 baseline values written out as app constants, so a library update cannot change them silently), chosen by `isSystemInDarkTheme()`, never `dynamicLightColorScheme`. `MainActivity` uses `enableEdgeToEdge()` with `SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)` for both bars, which keeps transparent scrims (the reason for the current light-only setting) while making the icons follow the dark theme.

### Layout
- `Scaffold` with a `TopAppBar` titled with the app name; content is a `LazyColumn` keyed by product id, with insets handled by the scaffold.
- Row: name (`titleMedium`, wraps), stock text (`bodyLarge`), then the date lines (`bodyMedium`, `onSurfaceVariant`). The "≈ " prefix uses `onSurfaceVariant`; zero stock uses `FontWeight.Bold` and `primary`. "Alert due" sits on a small `secondaryContainer` rounded surface (the soft highlight). Rows separated by `HorizontalDivider`. Each row merges its semantics so a screen reader reads it as one item, with the stock announced as "about N label" when rounded.
- Empty state: centered title (`titleMedium`) and help line (`bodyMedium`, `onSurfaceVariant`).
- All visible texts are string resources (ready for #48), with the number and dates as format arguments.
- Dates: `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` with the configuration locale. Numbers: `NumberFormat.getIntegerInstance(locale)` on the `BigInteger` whole units.

### Tests and evidence
- ViewModel unit tests with a fake `ProductRepository`, fixed clock and zone, and `kotlinx-coroutines-test` virtual time: ordering pass-through, empty, each row status, zone boundary, recompute after 60 s, store/change/remove re-emission.
- Robolectric Compose UI tests for the spec texts (en-US), the "≈" accessibility text and the no-tap behavior.
- Roborazzi references: empty state; a list covering whole, rounded, zero and large stock with scheduled, due and ran-out rows; the same list in dark; the same list at font scale 2.0 with a long name.
- E2E: seed products through the app's `AppContainer` repository, launch, assert the rows and the empty state on a clean install; the CI `e2e` video is the run evidence. Replace the placeholder tests (`PlaceholderScreenTest`, `PlaceholderScreenScreenshotTest` and its reference image); keep `LaunchTest` passing (the app name is still visible in the top bar).

## Risks / Trade-offs

- [The E2E seeds through the repository while the activity may already observe it] → Seed before launching the activity, and rely on the reactive list for any later write.
- [Roborazzi rendering of `≈` or dark scheme differs between local and CI] → Same JVM rendering in both (Robolectric native graphics); references are recorded once and verified pixel-exact as today.
- [A 60 s ticker keeps a coroutine alive while visible] → Negligible cost; stops at `STOPPED`. Footprint is measured later in #49.
- [Baseline colors look generic] → Accepted until #17 defines brand colors; the theme is one place to change.
