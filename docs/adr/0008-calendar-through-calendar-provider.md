# 0008. Calendar through the Android Calendar Provider

Date: 2026-09-22
Status: Accepted

## Context

Alerts can also be delivered as calendar events in the calendar the user already uses. This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

Use the Android Calendar Provider (on-device calendar, synced by the OS with the user's Google account). No OAuth, no Google Cloud project, no app verification. The user chooses which calendar receives events.

## Consequences

- The calendar channel is opt-in and needs explicit calendar permission.
- No Google Cloud project or app verification to maintain.
- Calendar Provider constraints are to be explored in a Phase 1 spike. Event details (title, time of day, reminders) are to be defined during specification.
