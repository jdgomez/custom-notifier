# 0014. Play App Signing with the upload key as a GitHub Actions secret

Date: 2026-09-29
Status: Accepted

## Context

Release builds must be signed, and agents must never handle keys ([WAYOFWORKING.md](../WAYOFWORKING.md#constraints-and-technical-decisions)). Signing keys are an owner-only step.

## Decision

Use Play App Signing. Google holds the app signing key; the owner holds the upload key, stored only as a GitHub Actions secret. Agents never see keys.

## Consequences

- Signing is set up in Phase 2 by the owner ([WAYOFWORKING.md](../WAYOFWORKING.md#phases)).
- The upload key never enters the repository or an agent session; it exists only as a GitHub Actions secret.
