## 1. Gate configuration

- [x] 1.1 Confirm the `.no-mistakes.yaml` schema for the installed version (commands, CI behavior) from its help, docs or source
- [x] 1.2 Add `.no-mistakes.yaml` with the lint and JVM test commands; do not set `ci.no_ci`

## 2. Pull request template

- [x] 2.1 Add `.github/pull_request_template.md` with the sections from design.md

## 3. Repository protection (prepared by the agent, applied by the owner)

- [x] 3.1 Add `.github/rulesets/main-protection.json` (pull request, required `verify` + `e2e` strict, no force push, no deletion, linear history, empty bypass list)
- [x] 3.2 Document in `AGENTS.md` the exact `gh api` commands to apply the ruleset and the merge settings (squash only, PR title as squash title, delete branch on merge, auto-merge off), plus the emergency procedure
- [x] 3.3 Remove the `--skip ci` bootstrap exception from `AGENTS.md` and state that the gate always waits on CI

## 4. Gate and merge

- [x] 4.1 Commit on `change/add-gate-and-repo-protection` with Conventional Commits
- [x] 4.2 Run the no-mistakes gate normally (CI step waits on `verify` and `e2e`) - gate passed on PR #33
- [x] 4.3 (owner) Merge, then apply the ruleset and merge settings with the documented commands - owner merged #33 (56e31e0) and applied the ruleset and merge settings on 2026-10-06; old ruleset 23779501 deleted by the owner

## 5. Proof after the owner applies protection

- [x] 5.1 Confirm via `gh api repos/{owner}/{repo}/rules/branches/main` that every expected rule applies to `main` (`pull_request`, `required_status_checks` with `verify` and `e2e`, `non_fast_forward`, `deletion`, `required_linear_history`), and take the `ruleset_id` of the `main-protection` ruleset from that output; read `gh api repos/{owner}/{repo}/rulesets/{ruleset_id}` and confirm `enforcement` is `active` (not `evaluate` or `disabled`) and `bypass_actors` is empty; confirm the merge settings via `gh api repos/{owner}/{repo}` (squash only, delete branch on merge); record the output in the change's handoff note. If anything is missing or not active, stop and escalate to the owner: do not run 5.2
  - Evidence (2026-10-06): `rules/branches/main` lists `pull_request`, `required_status_checks`, `non_fast_forward`, `deletion`, `required_linear_history`, all from ruleset_id 24587326. `rulesets/24587326`: name `main-protection`, `enforcement` `active`, `bypass_actors` `[]`; required checks `verify` and `e2e`, `strict_required_status_checks_policy` true. Repo: `allow_squash_merge` true, `allow_merge_commit` false, `allow_rebase_merge` false, `allow_auto_merge` false, `delete_branch_on_merge` true, `squash_merge_commit_title` PR_TITLE, `squash_merge_commit_message` COMMIT_MESSAGES.
- [x] 5.2 Only after 5.1 passes: confirm a direct push to `main` is rejected (attempt from a scratch commit that is never meant to land)
  - Evidence (2026-10-06): `git push origin <scratch-sha>:refs/heads/main` (scratch commit built with `git commit-tree` on top of `origin/main`, never on a branch) was rejected: `GH013: Repository rule violations found for refs/heads/main` - `Changes must be made through a pull request.` and `2 of 2 required status checks are expected.` (`[remote rejected] ... (push declined due to repository rule violations)`). `origin/main` unchanged at 56e31e0.
