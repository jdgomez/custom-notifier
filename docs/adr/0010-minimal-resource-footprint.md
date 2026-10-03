# 0010. Minimal resource footprint, measured

Date: 2026-09-22
Status: Accepted

## Context

The app should be light for the user's device. This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

Aim for minimal APK size, RAM and battery usage. These are to be measured, not assumed.

## Consequences

- Resource footprint measurement (APK size, RAM, background battery usage) is a Phase 1 task.
- Not decided here: numeric budgets. The source documents state none.
