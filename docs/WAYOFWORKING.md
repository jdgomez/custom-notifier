# WAYOFWORKING - custom-notifier
_Date: 2026-09-29_

This is a living document. It defines **how** custom-notifier is built, while `SESSION0.md` defines **what** is built. It is the reference for orchestrating and prioritizing work. Changes follow the process in [Evolving this document](#evolving-this-document).

## Problem
The app is built with agent engineering: the owner does not write the code. The owner is an experienced developer but new to native mobile development and to this way of working. We need a process that:
- Produces high-quality, reviewable changes without the owner writing code.
- Keeps the owner in control of the decisions that matter, and forces them to review each step so they learn.
- Makes the whole development history visible and understandable to anyone curious about how the project was built.
- Parallelizes work the way a human team would, when the owner decides to.

## Actors
| Actor | Role |
|------|------|
| Owner | The human. Approves proposals, UX, new dependencies and merges. Handles anything involving credentials, signing keys, payments and legal steps. |
| Brain | Orchestrator. Plans each change with the owner and writes the plan into the OpenSpec artifacts, manages the GitHub project, creates and tracks flows, escalates to the owner. The only agent that dispatches work. Does not implement or review code. |
| Executor | Implements the change on its branch and commits. |
| Reviewer | Validates the committed change through the no-mistakes gate, which ends with a pull request and green CI. Runs in its own pane, separate from the executor. Started on demand when the executor first reports its work done, and kept alive through the review rounds of the same change. |

## Vision
The owner starts a work session, tells Brain whether parallel work is allowed, approves the next change proposal, and then reviews a ready pull request with tests, green CI, screenshots and video, and merges it. Every step is traceable on GitHub from issue to spec to PR to merge. Nothing reaches `main` without passing the gate and the owner's review.

## Scope

### In
- OpenSpec as the source of truth for behavior; every piece of work is a change.
- Flows (executor and reviewer, one per change) created and tracked by Brain through Herdr.
- no-mistakes as the mandatory gate for every change.
- GitHub Actions CI running on every pull request.
- GitHub as the public project manager: milestones, issues, labels, project board.
- Architecture Decision Records (ADRs).
- Human checkpoints, escalation rules and a pause protocol.
- A brief retrospective at the end of each phase.

### Out
- Auto-merge of pull requests (for now).
- Parallel flows by default (the owner enables them per work session).
- Stacked pull requests.
- Agents handling secrets, signing keys, store accounts, payments or legal registrations.

### Pending
- A way to classify low-risk pull requests whose merge can be delegated.
- Automatic detection of the 85% account token usage threshold (e.g. via data exposed by Claude Code); manual for now.
- Whether E2E tests can move to stable releases only, based on experience with CI duration.

## Glossary
| Term | Agreed definition |
|------|-------------------|
| Flow | A workspace backed by its own git worktree and branch, with an executor and a reviewer, working on exactly one change. |
| Gate | The no-mistakes validation (review, tests, lint, docs, push, PR, CI) every change must pass before reaching `main`. |
| Change | Unit of work defined in OpenSpec (proposal, specs, design, tasks). 1 change = 1 branch = 1 PR = 1 squash commit on `main`. |
| Spike | A short exploratory change that answers a technical question. Its output is documented knowledge (and usually an ADR), not production code. |
| Human checkpoint | A step where no agent proceeds without explicit owner approval: OpenSpec proposal, UX, new dependency, merge. |
| Parallelizable | A change is parallelizable with another if it does not depend on it (neither needs the other merged first) and they do not touch the same areas of code. Each change declares `depends on:` and `touches:`. |
| Handoff note | A note inside the change folder with the exact state of the work (done, remaining, next step), so any agent can resume it without relying on conversation memory. |
| Definition of Done | The criteria a change must meet to be considered finished (see below). |
| ADR | Architecture Decision Record: a numbered document with context, decision and consequences for a significant technical decision. |

## Work unit and lifecycle
1. **Propose.** Brain writes an OpenSpec change (`openspec-propose`), declaring `depends on:` and `touches:`, and creates the matching GitHub issue in the phase milestone.
2. **Approve (human checkpoint).** The owner reviews and approves the proposal. The issue moves to Ready.
3. **Delegate.** Brain creates a flow for the change and hands it over. The issue moves to In progress.
4. **Implement.** The executor works on branch `change/<change-name>`, committing with Conventional Commits.
5. **Gate.** The reviewer runs no-mistakes. It ends with a pull request (title in Conventional Commits format, body with `Closes #N`) and green CI. The issue moves to In review.
6. **Review and merge (human checkpoint).** The owner reviews, including UX from screenshots and video, and squash-merges. GitHub closes the issue and moves it to Done.
7. **Archive.** The OpenSpec change is archived and the main specs are updated.
8. **Close.** Once the pull request is merged or the change is abandoned, Brain closes the flow: its worktree, workspace and status files are removed. Flows are never reused.

While a pull request waits for the owner, a sequential flow waits too (no stacking). When parallel work is enabled, other flows may continue with independent changes only.

## Definition of Done
A change is done only when all of these hold:
- Tests cover the new behavior.
- Lint passes with zero warnings (Android Lint, ktlint, detekt).
- The no-mistakes gate passes.
- CI is green.
- Documentation is updated where relevant (repo `AGENTS.md`, ADRs, specs).
- For user-visible changes: screenshots and a short video in the pull request. The video is recorded by the E2E tests in CI (no token cost; review is manual by the owner) and linked from the PR as a CI artifact.
- Size is reviewable in one sitting: about 400 lines of production code at most, excluding tests. Larger work is split into several changes.

## Development practices
- **Spec Driven Development (SDD)** is the backbone: OpenSpec defines behavior before any code is written, the change is implemented against it, and the spec is archived as the source of truth.
- **No test-first mandate.** Tests are required by the Definition of Done, but the order in which they are written is not prescribed. The auditable requirement is the outcome: tests that exercise real behavior (never assertions over source text), plus, for bug fixes, a test that reproduces the reported failure and fails before the fix.
- **BDD as vocabulary, not tooling.** Spec scenarios are written as Given / When / Then so the owner can validate behavior without reading code. No Cucumber/Gherkin layer: there are no non-technical stakeholders authoring scenarios, so the translation layer is pure maintenance cost.
- **Tactical DDD only.** The glossary in `SESSION0.md` is the ubiquitous language: glossary terms are the type names. Domain concepts are modelled as dedicated types (Product, Stock, PackageSize, ConsumptionRate, LeadTime) instead of bare primitives, so units, packages and days cannot be mixed up. Depletion logic lives in the domain, never in a ViewModel. No bounded contexts, event storming, CQRS or event sourcing: there is a single small domain.
- **Ports and adapters by construction.** The domain module is pure Kotlin with no Android dependencies; persistence, notifications and calendar are adapters it does not know about. This keeps domain tests fast (no emulator) and makes the logic portable to iOS later.
- These rules live in the repo `AGENTS.md`, which every agent reads, and are recorded as an ADR.

## Testing strategy
All levels run on every pull request:
- **Unit tests (JVM):** domain logic (stock, consumption rate, dates, rules).
- **UI tests:** Compose UI tests (Robolectric).
- **Screenshot tests:** Roborazzi, to catch any unintended visual change pixel by pixel.
- **E2E tests on a real emulator:** notifications, calendar, permissions. Record video.

## Git conventions
- One branch per change: `change/<change-name>`.
- Conventional Commits for all commits and PR titles.
- Squash merge only. The squash commit takes the PR title.
- `main` is protected: no direct pushes, PR required, CI must be green.
- CHANGELOG and versions are generated automatically from Conventional Commits (never edited by hand).

## Prioritization
Brain orders changes by:
1. **Dependencies first:** what unblocks the most work.
2. **Risk second:** the most uncertain work goes early (spikes), while changing course is still cheap.
3. **User value last.**

Only the owner decides, at the start of each work session, whether parallel flows are allowed. Brain then picks changes that are parallelizable with each other.

## Escalation
A flow stops and Brain brings the issue to the owner (label `needs-human` or `blocked` on the issue) when:
- An agent is blocked on a prompt or question.
- The gate fails 3 times in a row on the same change.
- A decision arises that the specs do not cover.

Agents never improvise answers to these situations.

## Pause protocol
Work sessions use the owner's normal account limits. When usage reaches **85% of the account token limit** (currently detected by the owner watching the screen, who tells Brain "pause"):
1. Brain notifies the executor and reviewer of every active flow.
2. Each agent finishes its current step at a consistent point.
3. They update the task checkboxes in the change's `tasks.md` and write a handoff note in the change folder.
4. They commit on the change branch.
5. They wait for Brain's instruction to resume.

## GitHub as project manager
The project board is public so anyone can follow how the project was built.

| OpenSpec / process | GitHub |
|------|------|
| Phase | Milestone |
| Change / spike | Issue, linking the change folder, with `depends on` / `touches` and a checklist mirroring the change's tasks at change level |
| Type and state | Labels: `change`, `spike`, `adr`, `parallelizable`, `blocked`, `needs-human` |
| Implementation | Pull request with `Closes #N` |
| Overview | Project board: Backlog, Ready, In progress, In review, Done |

Brain creates and moves issues. The reviewer (through no-mistakes) opens the pull request. GitHub closes the issue on merge. Tooling: `gh` CLI (scopes `repo`, `read:org`, `project`, `workflow`).

## Documentation
- **Repo `AGENTS.md`:** short; key commands and conventions for agents.
- **`SESSION0.md` and `WAYOFWORKING.md`:** reference documents; changed only with owner approval.
- **OpenSpec specs:** source of truth for behavior; changes are archived when completed.
- **ADRs:** `docs/adr/NNNN-title.md`, Nygard format (context, decision, consequences). The decisions already made in `SESSION0.md` become the first, retroactive ADRs.

## Phases
### Phase 0 - Foundation
Goal: everything needed so that product changes can flow through the full lifecycle, including a gate that waits on real CI.

Ordered changes (dependencies in parentheses):
1. **Local toolchain setup** - install JDK 17, Android SDK and an emulator image (KVM available) on the development machine, supervised by the owner. ()
2. **Android project skeleton** - Gradle with version catalog, Kotlin, Jetpack Compose, a pure Kotlin domain module separated from the UI, minimum API 26. (1)
3. **Lint setup** - Android Lint, ktlint, detekt, zero warnings. (2)
4. **Test infrastructure** - unit, Robolectric UI, Roborazzi screenshots, emulator E2E with video recording. (2)
5. **CI workflows** - GitHub Actions running build, lint and all test levels on every PR, uploading screenshots and video as artifacts. (3, 4)
6. **Gate and repo protection** - no-mistakes configured to wait on CI, `main` branch protection, PR template (Conventional Commits title, `Closes #N`, screenshots/video section). (5)
7. **Agent docs and ADRs** - repo `AGENTS.md`, `docs/adr/` with retroactive ADRs from `SESSION0.md`. (2; parallelizable with 3 and 4 since it touches docs only)

Changes 3 and 4 both touch the Gradle build, so they are not parallelizable with each other. Before change 5 exists, the gate's CI step has nothing to wait on; branch protection is enabled in change 6, once CI exists.

Alongside Phase 0, Brain sets up GitHub: labels, milestones per phase, and the project board.

Success criterion: a trivial change goes through the full lifecycle (proposal, issue, flow, gate, PR with green CI including E2E and video, owner merge, archive).

### Phase 1 - MVP (Android)
Product scope as defined in `SESSION0.md`. Planned order following the prioritization rules:
spike (exact alarms and Calendar Provider constraints) → domain → persistence → UI → notifications → calendar → Spanish translation → resource footprint measurement → 8-week validation with the real tester.

### Phase 2 - Public release hardening
As defined in `SESSION0.md`. Owner-only steps: signing keys (Play App Signing, upload key stored as a GitHub Actions secret), registration of rights over the app before publishing to any store, store accounts and payments.

### Phase 3 - iOS
As defined in `SESSION0.md`, in a separate repository following this same way of working.

### Final phase - Environment cleanup
Once a stable version is released (not just the MVP): uninstall the local toolchain installed in Phase 0 (JDK, Android SDK, emulator images).

### Retrospectives
At the end of each phase, a brief retrospective reviews what worked in the process and proposes updates to this document.

## Constraints and technical decisions
- Agent pipeline: Brain / Executor / Reviewer. Each change gets its own flow, backed by its own git worktree, with the executor and reviewer in separate Herdr panes. Whether flows may run in parallel is still decided by the owner per work session.
- History: a Planner agent existed in the original design, until 2026-09-29. It did not work as needed: planning decisions (scope, task breakdown, acceptance criteria) belong in the conversation between Brain and the owner and must be written into the OpenSpec artifacts before implementation, so the workers can act on them without anyone relaying context. A separate Planner between Brain and the workers added a relay hop and a second place where the plan could drift, without adding anything Brain and the owner do not already do. It was retired; see the agent engineering workflow ADR.
- no-mistakes is already initialized in this repository.
- The repository is public on GitHub, so GitHub Actions minutes (including Linux runners with emulator support) are free.
- Development machine: JDK 21 and Android SDK installed in Phase 0 (see `docs/development-setup.md`); KVM available for hardware-accelerated emulation; 16 cores, 31 GB RAM.
- Signing: Play App Signing. Google holds the app signing key; the owner holds the upload key, stored only as a GitHub Actions secret. Agents never see keys.
- No personal references in any project artifact.
- All project artifacts in English.

## Evolving this document
Brain proposes changes when experience calls for it (for example delegating low-risk merges, moving E2E to releases, parallel by default). The owner approves them. Changes with technical impact also get an ADR. Retrospectives are the natural moment to review this document.

## Open questions
- Criteria for low-risk pull requests eligible for delegated merge.
- How to detect account token usage automatically for the pause protocol.
- Exact mechanism to surface screenshots in the PR body (artifact links vs committed images).
- CI duration with E2E on every PR, and whether it becomes a bottleneck.
