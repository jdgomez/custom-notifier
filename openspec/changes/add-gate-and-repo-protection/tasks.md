## 1. Gate configuration

- [ ] 1.1 Confirm the `.no-mistakes.yaml` schema for the installed version (commands, CI behavior) from its help, docs or source
- [ ] 1.2 Add `.no-mistakes.yaml` with the lint and JVM test commands; do not set `ci.no_ci`

## 2. Pull request template

- [ ] 2.1 Add `.github/pull_request_template.md` with the sections from design.md

## 3. Repository protection (prepared by the agent, applied by the owner)

- [ ] 3.1 Add `.github/rulesets/main-protection.json` (pull request, required `verify` + `e2e` strict, no force push, no deletion, linear history, empty bypass list)
- [ ] 3.2 Document in `AGENTS.md` the exact `gh api` commands to apply the ruleset and the merge settings (squash only, PR title as squash title, delete branch on merge, auto-merge off), plus the emergency procedure
- [ ] 3.3 Remove the `--skip ci` bootstrap exception from `AGENTS.md` and state that the gate always waits on CI

## 4. Gate and merge

- [ ] 4.1 Commit on `change/add-gate-and-repo-protection` with Conventional Commits
- [ ] 4.2 Run the no-mistakes gate normally (CI step waits on `verify` and `e2e`)
- [ ] 4.3 (owner) Merge, then apply the ruleset and merge settings with the documented commands

## 5. Proof after the owner applies protection

- [ ] 5.1 Confirm a direct push to `main` is rejected (dry attempt from a scratch commit that is never meant to land)
- [ ] 5.2 Confirm the settings via `gh api` (ruleset active, squash only, delete branch on merge) and record the output in the change's handoff note
