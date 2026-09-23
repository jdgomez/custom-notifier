## Context

`docs/SESSION0.md` ("Constraints and technical decisions") and `docs/WAYOFWORKING.md` ("Development practices", "Constraints and technical decisions", "Documentation") already contain the decisions. `WAYOFWORKING.md` fixes the ADR format (Nygard) and location (`docs/adr/NNNN-title.md`). `AGENTS.md` is intentionally short.

## Goals / Non-Goals

**Goals:**
- A faithful, retroactive record: context and consequences taken from the source documents, plus the alternatives those documents mention.

**Non-Goals:**
- ADR tooling (adr-tools or similar). Plain Markdown files and a hand-maintained index are enough at this size.

## Decisions

### Plain Nygard ADRs with a date line
Each file uses the title `# NNNN. Title`, then `Date:` and `Status:` lines, then Context / Decision / Consequences. Retroactive ADRs use the date of the source decision (2026-09-22) and add one sentence stating that they are retroactive, with links to the source section.

### One decision per ADR
Bundling unrelated decisions would make superseding one of them awkward. The one exception is the development practices ADR: `WAYOFWORKING.md` asks for these to be recorded as a single ADR, because they form one coherent approach. If one practice changes later, a new ADR supersedes the bundle and restates the practices that remain.

### `AGENTS.md` stays short
Add a "Development practices" section of a few bullets (each links to its ADR) and an "Architecture decisions" section with the ADR rule and the index link. Do not duplicate the ADR content.

## Risks / Trade-offs

- [A retroactive ADR adds rationale that was never agreed] → The only content allowed is what the source documents state. Anything else goes under Consequences as "Not decided here", or is omitted. The owner checks this when approving the pull request.
- [`AGENTS.md` merge conflicts with the lint and test changes] → Keep additions in separate sections from the command lines those changes add.
