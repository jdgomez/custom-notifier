# 0004. Minimum Android version 8.0 (API 26)

Date: 2026-09-22
Status: Accepted

## Context

The app needs a minimum supported Android version. This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

The minimum Android version is 8.0 (API 26).

## Consequences

- `minSdk` is 26 in the Gradle build.
- Not decided here: the reasons for choosing API 26 are not stated in the source documents.
