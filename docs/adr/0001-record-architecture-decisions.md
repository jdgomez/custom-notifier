# 0001. Record architecture decisions

Date: 2026-09-29
Status: Accepted

## Context

Significant technical decisions were spread through the prose of [SESSION0.md](../SESSION0.md) and [WAYOFWORKING.md](../WAYOFWORKING.md), with no record of alternatives or consequences. Agents reading the code later cannot tell a deliberate decision from an accident. [WAYOFWORKING.md](../WAYOFWORKING.md#documentation) requires ADRs in `docs/adr/NNNN-title.md`, Nygard format, and says the decisions already made in `SESSION0.md` become the first, retroactive ADRs.

## Decision

We record significant technical decisions as ADRs in `docs/adr/`, using the Nygard format (Title, Status, Context, Decision, Consequences) plus a date line. Files are numbered sequentially and numbers are never reused. A hand-maintained index in [README.md](README.md) lists every ADR with its status. An accepted ADR is never rewritten: a changed decision is a new ADR that supersedes the old one. No ADR tooling is used; plain Markdown is enough at this size.

A significant technical decision (new architectural pattern, new module, new dependency with lasting impact, or reversal of an existing ADR) requires an ADR in the same change. The repo `AGENTS.md` states this rule.

## Consequences

- Reviewers can reject an undocumented significant technical decision.
- The index and the ADR must be kept in sync in the same change.
- The retroactive ADRs 0002 to 0014 record only what the source documents state.
