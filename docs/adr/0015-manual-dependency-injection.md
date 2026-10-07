# 0015. Manual dependency injection

Date: 2026-10-07
Status: Accepted

## Context

The app needs shared, long-lived dependencies (the database and the product repository now; a clock, a time zone provider and the alert scheduler in later changes). The app is small, has one module with Android code, and favors a minimal footprint ([0010](0010-minimal-resource-footprint.md)) and ports and adapters ([0012](0012-development-practices.md)).

## Decision

Build dependencies by hand. `CustomNotifierApplication` owns one `AppContainer` that lazily creates the Room database and exposes the dependencies as domain ports (for example `productRepository: ProductRepository`). ViewModels receive what they need through a factory that reads the container.

## Alternatives considered

- **Hilt:** annotation processing, a Gradle plugin and generated code; heavy for one database and a handful of classes.
- **Koin:** resolves at run time, so a missing binding fails when the app runs instead of when it compiles.

## Consequences

- Wiring is plain Kotlin: compile-time checked, no generated code, no extra dependency.
- Every new dependency is added to the container by hand.
- Revisit when the container becomes hard to maintain by hand (many scopes, many dependencies, or a need for per-screen lifetimes); then supersede this ADR with one choosing a framework.
