# 0003. One repository per platform

Date: 2026-09-22
Status: Accepted

## Context

The product has a native app per platform ([0002](0002-native-apps-per-platform.md)). This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

Two repositories, one per platform. This repository is the Android app. The iOS app lives in a separate repository and follows the same way of working (see [WAYOFWORKING.md](../WAYOFWORKING.md#phases)).

## Consequences

- This repository contains only the Android app.
- The iOS app (Phase 3) is built in a separate repository following this same way of working.
