## Why

The significant technical decisions made in `SESSION0.md` and `WAYOFWORKING.md` are spread through prose, with no record of the alternatives or consequences. Agents reading the code later cannot tell a deliberate decision from an accident. `WAYOFWORKING.md` requires those decisions to become the first, retroactive Architecture Decision Records, and requires the development practices (SDD, BDD vocabulary, tactical DDD, ports and adapters) to be recorded as an ADR and stated in the repo `AGENTS.md`.

**Depends on:** `add-android-project-skeleton` (the ADRs and `AGENTS.md` describe a structure that must exist).
**Touches:** `docs/adr/` (new), `AGENTS.md`, `README.md` (docs link). Parallelizable with `add-lint-setup` and `add-test-infrastructure` (docs only). Those changes also add command lines to `AGENTS.md`, so whichever merges second rebases: an expected, trivial text conflict.

## What Changes

- `docs/adr/` with a README index and the Nygard format (Title, Status, Context, Decision, Consequences).
- ADR 0001 "Record architecture decisions", then retroactive ADRs, one per decision:
  - native apps per platform (Kotlin + Jetpack Compose on Android, no cross-platform framework)
  - one repository per platform
  - minimum Android 8.0 (API 26)
  - no backend, all data on the device
  - local SQLite storage in private app storage with parameterized queries
  - deterministic alert scheduling with the OS, no background polling
  - calendar through the Android Calendar Provider (no OAuth)
  - English as the development language, Spanish as a translation
  - development practices: SDD with OpenSpec, BDD as vocabulary only, tactical DDD with glossary terms as type names, ports and adapters with a pure Kotlin domain
  - agent engineering workflow: Brain/Planner/Executor/Reviewer flows, the no-mistakes gate, owner-only checkpoints
  - Play App Signing with the upload key only as a GitHub Actions secret
- `AGENTS.md` updated: development practices in short form, a link to the ADR index, the rule that significant technical decisions get a new ADR, and how to supersede one.

## Capabilities

### New Capabilities
- `architecture-decisions`: how significant technical decisions are recorded, numbered, changed and found.

### Modified Capabilities
- None.

## Non-goals

- New decisions. The retroactive ADRs record decisions already taken, with their original rationale, and do not revisit them.
- Choosing the persistence library. `SESSION0.md` says "such as Room", so the ADR records SQLite with parameterized queries as the decision, and leaves the library choice to the Phase 1 persistence change (its own ADR).
- Editing `SESSION0.md` or `WAYOFWORKING.md` (owner approval required). ADRs link to them.

## New dependencies (owner approval)

- None.

## Impact

- Documentation only. No build or code impact.
- From this change on, reviewers can reject an undocumented significant technical decision by pointing to the ADR rule in `AGENTS.md`.
