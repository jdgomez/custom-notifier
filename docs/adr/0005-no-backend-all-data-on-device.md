# 0005. No backend, all data on the device

Date: 2026-09-22
Status: Accepted

## Context

The MVP is single-user: no shared households, no accounts, no server ([SESSION0.md](../SESSION0.md#target-user)). This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

No backend. All data stays on the device. No accounts, no server costs.

## Consequences

- No sync, no shared households, no server-side features in the MVP.
- Data backup and export are out of the MVP; data loss is acceptable for it. They are pending before public release ([SESSION0.md](../SESSION0.md#pending)).
- No server costs or server security surface.
