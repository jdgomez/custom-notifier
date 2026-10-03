## Context

- no-mistakes v1.79 is initialized for this repository (gate remote `no-mistakes`). It reads repository config (`.no-mistakes.yaml`) only from the default branch, as a security measure.
- `no-mistakes axi run --skip ci` has been the bootstrap exception for changes merged before CI existed.
- The `verify` and `e2e` checks come from `add-ci-workflows`.
- Agents act through the owner's `gh` session (scopes include `repo` and `workflow`), so any protection that exempts administrators also exempts agents.

## Goals / Non-Goals

**Goals:**
- Enforcement by GitHub, not by agent instructions.
- The gate, CI and local runs use the same commands.

**Non-Goals:**
- CODEOWNERS. There is a single owner, so it would add nothing to the ruleset.

## Decisions

### Repository ruleset instead of classic branch protection
Rulesets are GitHub's current mechanism. They can be exported as JSON and applied with `gh api`, and they show bypass lists explicitly. The ruleset is `main-protection`, targeting the default branch, with rules:
- `pull_request` (0 required approvals, see proposal Non-goals)
- `required_status_checks` (`verify`, `e2e`, strict = up to date)
- `non_fast_forward`
- `deletion`
- `required_linear_history`

The bypass list is empty.
The ruleset JSON is committed as `.github/rulesets/main-protection.json`, so the protection itself is reviewable and reproducible.

### Applied by the owner after merge
Repository settings take effect immediately, not on merge. Applying them from the change branch would change `main`'s rules while the pull request that defines them is still under review. The owner runs the documented `gh api` commands (ruleset, merge settings, verification, removal of the old ruleset) after merging, and the executor prepares them in `AGENTS.md`. This is also the step that removes the agents' own ability to push to `main`, so a human should perform it.

### Replacing the existing ruleset
The repository already has a ruleset `block delete and force push` (id 23779501, active) whose `ref_name` include list is empty, so it protects no branch: `main` is currently unprotected. The owner deletes it (`gh api -X DELETE .../rulesets/23779501`) as the last apply step, after `main-protection` is created and verified active, so there is never a window with less protection than today.

### Merge settings
`delete_branch_on_merge` is on. It removes only the remote head branch; local branches and worktrees are untouched and the branch can be restored from the PR.

### `.no-mistakes.yaml` content
It declares the project's lint command (`./gradlew lintAll`) and test command (`./gradlew test`). It does not declare the E2E script: an emulator in the gate's disposable worktree would duplicate CI's `e2e` job and load the machine. E2E evidence comes from CI. The exact key names are confirmed against the installed no-mistakes version (`no-mistakes --help`, its docs or source) during implementation, not guessed. `ci.no_ci` is not set, so the CI step waits on real checks.

### Pull request template
`.github/pull_request_template.md` with sections:
- Summary
- OpenSpec change (path)
- `Closes #N`
- Test evidence
- Screenshots / video (user-visible changes; link to the CI `e2e` artifact)
- Checklist mirroring the Definition of Done

The no-mistakes PR step does not look for `.github/pull_request_template.md` on its own (v1.79 has only the explicit `pr.template` key). `.no-mistakes.yaml` therefore sets `pr.template: .github/pull_request_template.md`, which the gate reads from `main` and which must start with a top-level `# ` heading. Whether the generated body keeps `Closes #N` is confirmed in the proof run.

## Risks / Trade-offs

- [Required check names drift from CI job names] → Documented in `AGENTS.md` as a coupled pair. Renaming a job is a change that updates the ruleset JSON in the same pull request.
- [Strict up-to-date checks force re-runs when `main` moves] → This is intended with sequential flows. When parallel flows are enabled, the gate's CI monitor already rebases automatically when the base branch moves.
- [The owner is locked out in an emergency] → The ruleset can be temporarily disabled by the owner in the GitHub UI. This is a deliberate human action, documented in `AGENTS.md`.
