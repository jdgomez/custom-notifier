# 0006. Local SQLite storage with parameterized queries

Date: 2026-09-22
Status: Accepted

## Context

Data stays on the device ([0005](0005-no-backend-all-data-on-device.md)) and needs a local store. This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

Store data in SQLite in the app's private storage, using parameterized queries. Stored content is never interpreted or executed, which prevents injection through stored data.

## Consequences

- Data is private to the app.
- Not decided here: the persistence library. The source says "such as Room"; the choice is left to the Phase 1 persistence change, which gets its own ADR.
- Encryption at rest is pending before public release ([SESSION0.md](../SESSION0.md#pending)).
