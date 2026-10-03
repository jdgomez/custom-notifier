# 0003. One repository per platform

Date: 2026-09-22
Status: Accepted

## Context

Native apps per platform ([0002](0002-native-apps-per-platform.md)) release independently. This ADR is retroactive: it records a decision taken in [SESSION0.md](../SESSION0.md#constraints-and-technical-decisions) (2026-09-22) and does not revisit it.

## Decision

Two repositories, one per platform. This repository is the Android app. The iOS app lives in a separate repository and follows the same way of working (see [WAYOFWORKING.md](../WAYOFWORKING.md#phases)).

## Consequences

- Each platform has its own history, CI and releases.
- Shared knowledge (glossary, way of working) is carried by documents, not by shared code.
