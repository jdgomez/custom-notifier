# 0011. Distribution for testing through Google Play internal testing

Date: 2026-09-22
Status: Accepted

## Context

A real tester must use the app during the 8-week validation ([SESSION0.md](../SESSION0.md#phases)). This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

Distribute test builds through the Google Play Console internal testing track (one-time developer account fee).

## Consequences

- A Play developer account is needed; store accounts and payments are owner-only.
- The closed testing requirement for new personal developer accounts is a Phase 2 concern.
