## 1. Gate configuration

- [x] 1.1 Confirm the `.no-mistakes.yaml` schema for the installed version (commands, CI behavior) from its help, docs or source
- [x] 1.2 Add `.no-mistakes.yaml` with the lint and JVM test commands; do not set `ci.no_ci`

## 2. Pull request template

- [x] 2.1 Add `.github/pull_request_template.md` with the sections from design.md

## 3. Repository protection (prepared by the agent, applied by the owner)

- [ ] 3.1 Add `.github/rulesets/main-protection.json` (pull request, required `verify` + `e2e` strict, no force push, no deletion, linear history, empty bypass list)
- [ ] 3.2 Document in `AGENTS.md` the exact `gh api` commands to apply the ruleset and the merge settings (squash only, PR title as squash title, delete branch on merge, auto-merge off), plus the emergency procedure
- [ ] 3.3 Remove the `--skip ci` bootstrap exception from `AGENTS.md` and state that the gate always waits on CI

## 4. Gate and merge

- [ ] 4.1 Commit on `change/add-gate-and-repo-protection` with Conventional Commits
- [ ] 4.2 Run the no-mistakes gate normally (CI step waits on `verify` and `e2e`)
- [ ] 4.3 (owner) Merge, then apply the ruleset and merge settings with the documented commands

## 5. Proof after the owner applies protection

- [ ] 5.1 Confirm via `gh api repos/{owner}/{repo}/rules/branches/main` that every expected rule applies to `main` (`pull_request`, `required_status_checks` with `verify` and `e2e`, `non_fast_forward`, `deletion`, `required_linear_history`), and take the `ruleset_id` of the `main-protection` ruleset from that output; read `gh api repos/{owner}/{repo}/rulesets/{ruleset_id}` and confirm `enforcement` is `active` (not `evaluate` or `disabled`) and `bypass_actors` is empty; confirm the merge settings via `gh api repos/{owner}/{repo}` (squash only, delete branch on merge); record the output in the change's handoff note. If anything is missing or not active, stop and escalate to the owner: do not run 5.2
- [ ] 5.2 Only after 5.1 passes: confirm a direct push to `main` is rejected (attempt from a scratch commit that is never meant to land)
