# 0012. Development practices

Date: 2026-09-29
Status: Accepted

## Context

[WAYOFWORKING.md](../WAYOFWORKING.md#development-practices) asks for the development practices to be recorded as a single ADR, because they form one coherent approach, and to be stated in the repo `AGENTS.md`. This ADR records the practices as stated there.

## Decision

- **Spec Driven Development (SDD)** with OpenSpec: behavior is defined before any code, the change is implemented against it, and the spec is archived as the source of truth.
- **No test-first mandate.** Tests are required by the Definition of Done, but their order is not prescribed. The requirement is tests that exercise real behavior (never assertions over source text) and, for bug fixes, a test that reproduces the reported failure and fails before the fix.
- **BDD as vocabulary, not tooling.** Spec scenarios use Given / When / Then. No Cucumber or Gherkin layer: there are no non-technical stakeholders authoring scenarios, so it would be pure maintenance cost.
- **Tactical DDD only.** The glossary in [SESSION0.md](../SESSION0.md#glossary) is the ubiquitous language: glossary terms are the type names. Domain concepts are dedicated types (Product, Stock, PackageSize, ConsumptionRate, LeadTime) instead of bare primitives. Depletion logic lives in the domain, never in a ViewModel. No bounded contexts, event storming, CQRS or event sourcing: there is a single small domain.
- **Ports and adapters by construction.** The domain module is pure Kotlin with no Android dependencies; persistence, notifications and calendar are adapters it does not know about.

## Consequences

- Domain tests are fast (no emulator) and the logic is portable to iOS later.
- Units, packages and days cannot be mixed up.
- If one practice changes later, a new ADR supersedes this bundle and restates the practices that remain.
