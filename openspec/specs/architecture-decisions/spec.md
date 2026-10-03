# architecture-decisions Specification

## Purpose

Defines how significant technical decisions are recorded as Architecture Decision Records: where they live, their format and numbering, how they are superseded instead of rewritten, and how agents find them.

## Requirements

### Requirement: ADR location, format and numbering
Every Architecture Decision Record SHALL live in `docs/adr/` as `NNNN-kebab-case-title.md`, where `NNNN` is a zero-padded sequence number that is never reused. Each record SHALL have the sections Title, Status, Context, Decision and Consequences.

#### Scenario: New ADR
- **GIVEN** the highest existing ADR number is N
- **WHEN** a new decision is recorded
- **THEN** it is created as number N+1 with all five sections, and its status is `Proposed` or `Accepted`

### Requirement: ADR index
`docs/adr/README.md` SHALL list every ADR with its number, title and current status, and SHALL be updated in the same change that adds or supersedes an ADR.

#### Scenario: Finding a decision
- **GIVEN** an agent wants to know why the domain module has no Android dependencies
- **WHEN** it opens the ADR index
- **THEN** it finds the entry for the ports-and-adapters decision and follows it to the record

### Requirement: Accepted ADRs are not rewritten
Once an ADR is `Accepted`, its Context, Decision and Consequences SHALL NOT be edited. A changed decision SHALL be recorded as a new ADR that supersedes it. The old ADR's status SHALL become `Superseded by NNNN`, and the new one SHALL reference the old one.

#### Scenario: Reversing a decision
- **GIVEN** ADR 0005 is accepted
- **WHEN** the project decides differently
- **THEN** a new ADR records the new decision and references 0005, ADR 0005's status reads "Superseded by" the new number, and its other sections are unchanged

### Requirement: Foundational decisions recorded
The ADR set SHALL include one accepted ADR for each technical decision listed in `SESSION0.md` "Constraints and technical decisions" and for the development practices and agent workflow in `WAYOFWORKING.md`, each linking back to the document section it comes from.

#### Scenario: Coverage check
- **GIVEN** the list of decisions in `SESSION0.md` "Constraints and technical decisions" and the development practices section in `WAYOFWORKING.md`
- **WHEN** each technical decision is looked up in the ADR index
- **THEN** each one has an accepted ADR that links to its source section

### Requirement: Agents are told when to write an ADR
The repo `AGENTS.md` SHALL state that a significant technical decision (new architectural pattern, new module, new dependency with lasting impact, or reversal of an existing ADR) requires an ADR in the same change, and SHALL link to the ADR index.

#### Scenario: Change introduces a new module
- **GIVEN** a change that adds a new Gradle module for an adapter
- **WHEN** it is reviewed against `AGENTS.md`
- **THEN** the review expects an ADR in that change, and flags its absence
