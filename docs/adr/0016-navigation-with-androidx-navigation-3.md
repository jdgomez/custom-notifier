# 0016. Navigation with AndroidX Navigation 3

Date: 2026-10-07
Status: Accepted

## Context

The app grows from one screen to several (the product list, the product form now; editing and adjustments next). It needs navigation with the system back gesture, including predictive back, and a ViewModel per screen. It favors a minimal footprint ([0010](0010-minimal-resource-footprint.md)) and few dependencies.

## Decision

Use AndroidX Navigation 3 (`navigation3-runtime`, `navigation3-ui`) with `lifecycle-viewmodel-navigation3`. The back stack is a `mutableStateListOf` of plain Kotlin keys (`Destination`) held in memory by an activity-scoped ViewModel and rendered by `NavDisplay`; each screen's ViewModel is scoped to its back stack entry and cleared when the entry is popped.

The back stack is not restored after process death: when the system ends the process, the app starts again on the product list. This avoids `kotlinx-serialization` (a Gradle plugin and a runtime) and lets keys stay plain types. The only thing lost is the input of a short form. The owner decided this on 2026-10-07.

## Alternatives considered

- **Restoring the back stack with `rememberNavBackStack`:** survives process death, but needs the `kotlinx-serialization` plugin and runtime and `@Serializable` keys.
- **`navigation-compose` 2.x:** mature, but typed route arguments need the same serialization plugin and the library owns the back stack.
- **Hand-written navigation:** no dependency, but the predictive back animation would be ours to build and maintain.

## Consequences

- Keys and the back stack are plain Kotlin and unit-testable; screens receive callbacks, not a navigator.
- Unsaved input is lost when the process is ended in the background.
- Revisit when testers or a larger user base ask for restoration, or when a screen holds input that is costly to retype; then supersede this ADR with one adopting `rememberNavBackStack` and `kotlinx-serialization`.
