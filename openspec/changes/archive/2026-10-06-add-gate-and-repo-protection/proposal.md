## Why

With CI in place, the last missing parts of the lifecycle are enforcement and consistency. Today anyone with the owner's token (every agent, because they act through the owner's `gh` session) could push straight to `main`. The gate runs its own checks without knowing the project's commands. Pull requests have no fixed shape. This change makes "nothing reaches `main` without the gate, green CI and the owner's merge" a property of the repository instead of a convention.

**Depends on:** `add-ci-workflows`.
**Touches:** `.no-mistakes.yaml` (new), `.github/pull_request_template.md` (new), GitHub repository settings and `main` ruleset (applied by the owner after merge), `AGENTS.md`.

## What Changes

- `.no-mistakes.yaml` committed with the project's lint and test commands, so the gate runs the same checks as CI, with its CI step waiting on the real `verify` and `e2e` checks.
- A pull request template: `Closes #N`, OpenSpec change link, test evidence, and a screenshots/video section for user-visible changes. Pull request titles stay Conventional Commits (they become the squash commit on `main`), but the title is its own field, not part of the template.
- Repository settings: squash merge only (the squash commit takes the PR title), head branches deleted after merge, auto-merge disabled.
- A `main` ruleset: pull request required, `verify` and `e2e` required and up to date, no force pushes, no deletion, linear history, with no bypass, not even for administrators, because agents act through the owner's account.
- The removal of the `--skip ci` bootstrap exception for the gate, documented in `AGENTS.md`.

## Capabilities

### New Capabilities
- `change-integration`: the path a change must take to reach `main`, and what the repository enforces along it.

### Modified Capabilities
- None.

## Non-goals

- Required pull request approvals. Pull requests are opened from the owner's own account, and GitHub does not let an author approve their own pull request. The owner's merge is the review. This will be revisited if a separate bot account is ever introduced.
- Delegated merge for low-risk pull requests (an open question in `WAYOFWORKING.md`).
- Automated CHANGELOG and version generation from Conventional Commits. This is needed before the first release, and will be proposed as its own change before Phase 1 distribution.

## New dependencies (owner approval)

- None. no-mistakes is already installed and initialized for this repository.

## Impact

- After the ruleset is on, direct pushes to `main` fail for everyone, including the owner. Emergency changes also go through a pull request.
- Every later gate run takes the project's commands from `main`'s `.no-mistakes.yaml` (no-mistakes reads repository config only from the default branch), so the config takes effect only once this change is merged.
