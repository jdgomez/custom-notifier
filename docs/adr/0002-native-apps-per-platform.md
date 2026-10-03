# 0002. Native apps per platform

Date: 2026-09-22
Status: Accepted

## Context

Each OS has its own release cycle, requirements and vulnerabilities; one platform changing must not force a release of the other. Platform-specific code has given the best results in the owner's experience. This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

Native apps per platform, with no cross-platform framework. Android is written in Kotlin with Jetpack Compose; iOS will later use Swift and SwiftUI.

## Consequences

- Each platform releases independently.
- Double maintenance is the cost, to be mitigated with solid CI/CD.
- Business logic stays portable through a pure Kotlin domain (see [0012](0012-development-practices.md)).
