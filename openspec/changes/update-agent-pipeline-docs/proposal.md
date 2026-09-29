## Why

The agent pipeline changed: the Planner agent did not work as needed and was retired. Brain now plans each change with the owner up front, writes the plan into the OpenSpec artifacts, and dispatches every handoff itself to an executor and a reviewer. `WAYOFWORKING.md`, the OpenSpec project context and the planned agent workflow ADR still describe the old Brain / Planner / Executor / Reviewer pipeline, so any agent or reader following them gets the process wrong. The decision affects how this project is built, so it must be recorded in the project, not only in the agents' own definitions.

**Depends on:** nothing.
**Touches:** `docs/WAYOFWORKING.md`, `openspec/config.yaml`, `openspec/changes/add-agent-docs-and-adrs/` (proposal, design, tasks). Documentation only. Parallelizable with `add-android-project-skeleton` (disjoint files). `add-agent-docs-and-adrs` should start after this change merges, so its ADR is written from the updated artifacts.

## What Changes

- `docs/WAYOFWORKING.md` describes the current pipeline (owner-approved change to this document):
  - Actors: the Planner row is removed. Brain plans each change with the owner and is the only agent that dispatches work; it still does not implement or review code. The reviewer is started on demand, when the executor first reports its work done, and stays alive through the review rounds of the same change.
  - Glossary "Flow": one Herdr workspace backed by its own git worktree and branch, with an executor and a reviewer, for exactly one change.
  - Lifecycle: "Delegate" creates the flow; the "Reset" step becomes "Close": the flow (worktree, workspace, status files) is closed once the pull request is merged or the change is abandoned. Flows are never reused.
  - Scope, pause protocol and constraints: every Planner reference is removed; the "one flow to start" constraint is replaced by the worktree-per-flow model. Whether parallel flows are allowed is still decided by the owner per work session.
  - A short note that a Planner agent existed until 2026-09-29 and why it was retired, pointing to the agent workflow ADR.
- `openspec/config.yaml`: the project context describes Brain planning with the owner and an Executor/Reviewer flow.
- `add-agent-docs-and-adrs` (not yet implemented): its agent engineering workflow ADR records the current Brain / Executor / Reviewer pipeline and states in its Context that a Planner agent was part of the original design and was retired, with the reason.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. This is a documentation and process change with no spec-level behavior; the change sets `skip_specs: true`.

## Non-goals

- Changing the agents' own definitions (`~/.claude/agents/`): they live outside this repository and are already updated.
- Writing the ADR itself or creating `docs/adr/`: that stays in `add-agent-docs-and-adrs` (#9).
- Changing any other part of the way of working (gate, Definition of Done, parallel-flow policy, escalation rules).
- Archiving `setup-local-toolchain`, which is still pending as a separate step.

## New dependencies (owner approval)

- None.

## Impact

- Documentation and planning artifacts only. No build or code impact.
- From this change on, the documents agree with how flows are actually run.
