## 1. Way of working

- [ ] 1.1 Update `docs/WAYOFWORKING.md` Actors: remove the Planner row; rewrite the Brain row (plans each change with the owner, only agent that dispatches work, does not implement or review code) and the Reviewer row (started on demand when the executor first reports done, kept alive through the change's review rounds)
- [ ] 1.2 Update Scope "In", the Glossary entry "Flow" (workspace backed by its own git worktree and branch, executor and reviewer, one change) and the Pause protocol so none of them mentions a planner
- [ ] 1.3 Update the lifecycle: step 3 "Delegate" creates the flow; step 8 "Reset" becomes "Close" (worktree, workspace and status files removed after merge or abandonment; flows are never reused)
- [ ] 1.4 Update "Constraints and technical decisions": pipeline Brain / Executor / Reviewer with one worktree-backed flow per change, replacing "one flow to start"; add the short history note on the retired Planner (see design.md, "Reason for the retirement"); keep the owner's per-session decision on parallel flows unchanged
- [ ] 1.5 Update the document date line

## 2. OpenSpec context

- [ ] 2.1 Update the context in `openspec/config.yaml`: Brain plans each change with the owner, an Executor/Reviewer flow implements and validates it

## 3. Agent workflow ADR plan

- [ ] 3.1 Update `openspec/changes/add-agent-docs-and-adrs/proposal.md`: the agent engineering workflow ADR records the Brain / Executor / Reviewer pipeline and the retired Planner
- [ ] 3.2 Add a decision to `openspec/changes/add-agent-docs-and-adrs/design.md`: the workflow ADR is dated 2026-09-29, describes the current pipeline, and its Context states that the original design included a Planner and why it was retired, quoting `WAYOFWORKING.md`
- [ ] 3.3 Update task 2.12 in `openspec/changes/add-agent-docs-and-adrs/tasks.md` to match

## 4. Verification and gate

- [ ] 4.1 Search the repository for "planner" (case-insensitive), excluding `.git/`, `openspec/changes/archive/` and this change's folder; the only hits are the history note in `WAYOFWORKING.md` and the ADR plan in `add-agent-docs-and-adrs`
- [ ] 4.2 Run `openspec validate update-agent-pipeline-docs` and `openspec validate add-agent-docs-and-adrs`
- [ ] 4.3 Commit on `change/update-agent-pipeline-docs` with Conventional Commits (`docs:`)
- [ ] 4.4 Run the no-mistakes gate with `--skip ci` (`add-ci-workflows` is not merged yet); the PR body is in English and contains `Closes #N`
