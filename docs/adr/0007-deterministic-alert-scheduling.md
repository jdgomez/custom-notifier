# 0007. Deterministic alert scheduling with the OS

Date: 2026-09-22
Status: Accepted

## Context

The depletion date is deterministic, given the estimated stock and the consumption rate. Battery usage must stay near zero. This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

Alerts are scheduled once with the OS and rescheduled only when the user changes something. There is no background polling.

## Consequences

- Battery usage stays near zero.
- Every user change that affects the depletion date must trigger a reschedule.
- Exact alarm constraints are to be explored in a Phase 1 spike ([WAYOFWORKING.md](../WAYOFWORKING.md#phases)).
