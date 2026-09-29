## 1. ADR structure

- [ ] 1.1 Create `docs/adr/README.md` (format, numbering, superseding rule, index table)
- [ ] 1.2 Write ADR 0001 "Record architecture decisions"

## 2. Retroactive ADRs

- [ ] 2.1 Native apps per platform (Kotlin + Jetpack Compose, no cross-platform framework)
- [ ] 2.2 One repository per platform
- [ ] 2.3 Minimum Android 8.0 (API 26)
- [ ] 2.4 No backend; all data on the device
- [ ] 2.5 Local SQLite in private storage with parameterized queries (library choice left to Phase 1)
- [ ] 2.6 Deterministic alert scheduling with the OS, no background polling
- [ ] 2.7 Calendar through the Android Calendar Provider
- [ ] 2.8 English development language, Spanish as translation
- [ ] 2.9 Resource footprint: minimal APK size, RAM and battery, measured rather than assumed
- [ ] 2.10 Distribution for testing through the Google Play internal testing track
- [ ] 2.11 Development practices: SDD, BDD vocabulary, tactical DDD, ports and adapters
- [ ] 2.12 Agent engineering workflow and the no-mistakes gate (dated 2026-09-29; describes the Brain / Executor / Reviewer pipeline; its Context states that the original design included a Planner and why it was retired, quoting `WAYOFWORKING.md`)
- [ ] 2.13 Play App Signing; upload key only as a GitHub Actions secret
- [ ] 2.14 Cross-check: every decision in the source sections has an ADR, and the index lists all of them

## 3. Agent docs

- [ ] 3.1 Update `AGENTS.md`: development practices (short, linked to ADRs), ADR rule and index link
- [ ] 3.2 Link `docs/adr/` from `README.md`

## 4. Gate

- [ ] 4.1 Commit on `change/add-agent-docs-and-adrs` with Conventional Commits
- [ ] 4.2 Run the no-mistakes gate with `--skip ci` if `add-ci-workflows` is not yet merged, otherwise normally
