# 0013. Agent engineering workflow and the no-mistakes gate

Date: 2026-09-29
Status: Accepted

## Context

The app is built with agent engineering: the owner does not write the code, is an experienced developer but new to native mobile development, and must stay in control of the decisions that matter ([WAYOFWORKING.md](../WAYOFWORKING.md#problem)).

The original design included a Planner agent, until 2026-09-29. [WAYOFWORKING.md](../WAYOFWORKING.md#constraints-and-technical-decisions) records why it was retired:

> It did not work as needed: planning decisions (scope, task breakdown, acceptance criteria) belong in the conversation between Brain and the owner and must be written into the OpenSpec artifacts before implementation, so the workers can act on them without anyone relaying context. A separate Planner between Brain and the workers added a relay hop and a second place where the plan could drift, without adding anything Brain and the owner do not already do. It was retired.

## Decision

The pipeline is Brain / Executor / Reviewer:

- **Brain** plans each change with the owner, writes the plan into the OpenSpec artifacts, manages the GitHub project, creates and tracks flows, and escalates to the owner. It is the only agent that dispatches work, and it does not implement or review code.
- **Executor** implements the change on branch `change/<change-name>` and commits with Conventional Commits.
- **Reviewer** validates the committed change through the no-mistakes gate, which ends with a pull request and green CI. It runs in its own pane, separate from the executor.

Each change gets its own flow, backed by its own git worktree. 1 change = 1 branch = 1 PR = 1 squash commit on `main`. Nothing reaches `main` without passing the no-mistakes gate and the owner's review. Owner-only checkpoints: OpenSpec proposal, UX, new dependencies, merge, and anything involving credentials, signing keys, payments and legal steps. A flow stops and escalates when an agent is blocked, when the gate fails 3 times in a row on the same change, or when a decision is not covered by the specs. Whether flows may run in parallel is decided by the owner per work session.

## Consequences

- The plan lives in one place, the OpenSpec artifacts, so workers do not depend on relayed context.
- The owner reviews every merge, including UX from screenshots and video.
- The Planner is not part of the pipeline. The history is recorded here and not as a separate superseding ADR.
- Auto-merge, parallel flows by default and stacked pull requests are out for now.
