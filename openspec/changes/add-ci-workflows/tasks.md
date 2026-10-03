## 1. Workflow

- [x] 1.1 Resolve the current release commit SHA of each approved action; do not use any action outside the approved list without escalating
- [x] 1.2 Create `.github/workflows/ci.yml` with triggers, concurrency and `contents: read` permissions
- [x] 1.3 Add the `verify` job (Java 21, Gradle setup with wrapper validation, build + lint + JVM tests, always upload reports and screenshot diffs, 14-day retention)
- [x] 1.4 Add the `e2e` job (KVM enable, emulator runner with the same API level and image as the local `cn-api<N>` AVD, cold boot on every run, `scripts/e2e.sh`, always upload `build/e2e/`, 14-day retention)

## 2. Proof on GitHub

- [ ] 2.1 Open the pull request through the gate (without `--skip ci`) and confirm both checks run and pass
- [ ] 2.2 Confirm the video artifact downloads and plays; link it in the PR description
- [x] 2.3 Prove failure paths on a throwaway branch `scratch/ci-failure-proof` (owner-approved one-off exception to "only the gate pushes"; draft PR closed unmerged, branch kept for the owner to delete): a lint finding fails `verify` and a broken `LaunchTest` assertion fails `e2e`, and both still upload their artifacts; link the runs in the PR description

## 3. Docs and gate

- [x] 3.1 Document the CI checks, their artifacts and the "check names are stable" rule in `AGENTS.md`
- [x] 3.2 Commit on `change/add-ci-workflows` with Conventional Commits
- [ ] 3.3 Run the no-mistakes gate normally; its CI step waits on `verify` and `e2e`
