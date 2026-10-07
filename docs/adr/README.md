# Architecture Decision Records

Significant technical decisions are recorded here as Architecture Decision Records (ADRs), in the Nygard format. The format and location are set in [`WAYOFWORKING.md`](../WAYOFWORKING.md#documentation).

## Format

- One file per decision: `NNNN-kebab-case-title.md`. `NNNN` is a zero-padded sequence number that is never reused.
- Title line `# NNNN. Title`, then `Date:` and `Status:` lines, then the sections Context, Decision and Consequences.
- Status is `Proposed`, `Accepted` or `Superseded by NNNN`.
- Retroactive ADRs use the date of the source decision and link to its section.

## Changing a decision

An accepted ADR is not rewritten. To change a decision, add a new ADR that references the old one, and set the old one's status to `Superseded by NNNN`. Its other sections stay unchanged.

The index below is updated in the same change that adds or supersedes an ADR.

## Index

| Number | Title | Status |
|--------|-------|--------|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-native-apps-per-platform.md) | Native apps per platform | Accepted |
| [0003](0003-one-repository-per-platform.md) | One repository per platform | Accepted |
| [0004](0004-minimum-android-version.md) | Minimum Android version 8.0 (API 26) | Accepted |
| [0005](0005-no-backend-all-data-on-device.md) | No backend, all data on the device | Accepted |
| [0006](0006-local-sqlite-storage.md) | Local SQLite storage with parameterized queries | Accepted |
| [0007](0007-deterministic-alert-scheduling.md) | Deterministic alert scheduling with the OS | Accepted |
| [0008](0008-calendar-through-calendar-provider.md) | Calendar through the Android Calendar Provider | Accepted |
| [0009](0009-english-development-language.md) | English development language, Spanish as a translation | Accepted |
| [0010](0010-minimal-resource-footprint.md) | Minimal resource footprint, measured | Accepted |
| [0011](0011-internal-testing-distribution.md) | Distribution for testing through Google Play internal testing | Accepted |
| [0012](0012-development-practices.md) | Development practices | Accepted |
| [0013](0013-agent-engineering-workflow.md) | Agent engineering workflow and the no-mistakes gate | Accepted |
| [0014](0014-play-app-signing.md) | Play App Signing with the upload key as a GitHub Actions secret | Accepted |
| [0015](0015-manual-dependency-injection.md) | Manual dependency injection | Accepted |
| [0016](0016-navigation-with-androidx-navigation-3.md) | Navigation with AndroidX Navigation 3 | Accepted |
