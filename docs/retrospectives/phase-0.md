# Phase 0 retrospective - Foundation

Date: 2026-10-06

## What worked
- One change = one branch = one PR, each with its own worktree flow. Parallel flows did not collide.
- CI with emulator E2E and the `main` ruleset. A direct push to `main` was rejected with GH013, proven in `add-gate-and-repo-protection`.
- Delegating investigations to executors (CI emulator research, the E2E false green #25, spike #28) gave clear conclusions while keeping the orchestrator context small.
- The Phase 0 success criterion was met: PR #35 went through the full lifecycle with the protection active.

## What did not work
- **Gate-generated PR bodies.** They dropped `Closes #N` or left the template placeholder literal, so most PRs needed manual fixes. The title heading in the PR template made the gate's PR step refuse to publish until PR #34.
- **Gate document step.** It makes unrequested doc edits and once weakened a spec requirement (Conventional Commits PR titles, PR #34).
- **Reviewer agent.** Running on a small model without auto mode, it stopped often on permission prompts (the owner had to be at the machine) and improvised: a manual push attempt, polling loops, a `gh api` call with `-f`.
- **E2E false green (#25).** Found late: the script ran before the emulator finished booting.

## Actions
- Brain checks the gate's commits and the PR title and body before handing a PR to the owner. Temporary: to be removed as soon as the gate is reliable, because it is a load on the orchestrator.
- Spike closure rule added to `docs/WAYOFWORKING.md`: findings posted on the issue, issue closed, follow-up issues created.
- Reviewer model raised from haiku to sonnet, under observation.
- A reviewer-only read-only GitHub token is being evaluated.
- How to show screenshots in PRs is decided in the first UI change.
- Follow-up issue #36 tracks the Gradle 11 deprecations found in spike #28.
