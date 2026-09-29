## Context

See proposal.md - Why. The current pipeline is defined by the agent definitions outside the repository (Brain, executor, reviewer). The repository documents that must match it are `docs/WAYOFWORKING.md` (Actors, Scope, Glossary, Work unit and lifecycle, Pause protocol, Constraints), `openspec/config.yaml` (context) and the unimplemented change `add-agent-docs-and-adrs`, whose task 2.12 plans the agent workflow ADR. `add-agent-docs-and-adrs` requires retroactive ADRs to contain only what the source documents state.

## Goals / Non-Goals

**Goals:**
- After this change, no project document describes a Planner as part of the current pipeline.
- The retirement of the Planner, and its reason, are traceable from `WAYOFWORKING.md` and from the planned ADR.

**Non-Goals:**
- Documenting Herdr commands, status files or completion lines. Those are agent-internal mechanics and stay in the agent definitions.

## Decisions

### Rewrite `WAYOFWORKING.md` in place, with one short history note
The document describes how the project is built today, so every section is rewritten to the current pipeline rather than annotated. A single short note (in "Constraints and technical decisions") records that a Planner agent existed until 2026-09-29 and was retired, so readers of the earlier history (PR #2, the Phase 0 proposals) are not confused. The document date line is updated to the date of the change.
- Alternative: a changelog-style section listing every edit. Rejected: git history already records the edits, and the document would accumulate noise.

### Reason for the retirement, as agreed with the owner
The Planner did not work as needed. Planning decisions (scope, task breakdown, acceptance criteria) belong in the conversation between Brain and the owner, and they must be written into the OpenSpec artifacts before implementation so the workers can act on them without anyone relaying context. A separate Planner agent between Brain and the workers added an extra relay hop and a second place where the plan could drift, without adding anything Brain and the owner do not already do. This wording is the source the ADR quotes.

### One agent workflow ADR describing the current pipeline
`add-agent-docs-and-adrs` has not been implemented and no ADR exists yet, so there is nothing accepted to supersede. Its agent workflow ADR records the current pipeline, dated 2026-09-29 (the date of the current decision, not 2026-09-22), and its Context states that the original design (2026-09-22) included a Planner and why it was retired. The other retroactive ADRs keep the date 2026-09-22.
- Alternative: two ADRs, one retroactive for the original Planner pipeline and one superseding it. Rejected: it would create an ADR that is superseded on the day it is written, only to describe a pipeline nobody follows any more; the Context section carries the history just as well.

### Edit `add-agent-docs-and-adrs` artifacts as part of this change
Its proposal (What Changes), design (a decision on the workflow ADR's date and content) and task 2.12 are updated here, so that when that change is implemented its executor finds the right instructions in the files. Issue #9's body has no Planner reference and needs no edit.

## Risks / Trade-offs

- [Another document or change mentions the Planner and is missed] → The last task is a repository-wide search for "planner" (case-insensitive) outside `openspec/changes/archive/` and this change's own folder; the only allowed hits are the history note and the ADR plan.
- [`WAYOFWORKING.md` changes need owner approval] → Approving this proposal is that approval for the edits listed; anything beyond them is escalated.
