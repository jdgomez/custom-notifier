## Why

Nothing checks a pull request independently of the machine that produced it. The Definition of Done requires green CI, and the no-mistakes gate's CI step needs real GitHub checks to wait on (without them it waits forever, as a live test on this repository showed). The owner's review also depends on screenshots and an E2E video that CI produces without spending agent tokens.

**Depends on:** `add-lint-setup`, `add-test-infrastructure`.
**Touches:** `.github/workflows/` (new), `AGENTS.md`.

## What Changes

- A GitHub Actions workflow that runs on every pull request and on every push to `main`, with two jobs:
  - **verify**: build, the full lint command, and JVM tests including screenshot verification.
  - **e2e**: boots a KVM-accelerated emulator on a Linux runner and runs the E2E entry point.
- Test reports, screenshot comparison images and the E2E video are uploaded as workflow artifacts, including when a job fails.
- Stable job names that the next change (`add-gate-and-repo-protection`) will mark as required status checks.
- Least-privilege workflow permissions and third-party actions pinned to commit SHAs.

## Capabilities

### New Capabilities
- `continuous-integration`: when CI runs, what it checks, which checks a pull request must pass, and which evidence it publishes.

### Modified Capabilities
- None.

## Non-goals

- Branch protection and required checks (`add-gate-and-repo-protection`).
- Release builds, signing or publishing to Google Play (Phase 2; the signing key is an owner-only secret).
- Automated dependency updates (Dependabot/Renovate). This can be proposed later as its own change.
- Posting screenshots inline in the PR body (an open question in `WAYOFWORKING.md`; artifact links for now).

## New dependencies (owner approval)

GitHub Actions, each pinned to a full commit SHA with the version in a comment:
- `actions/checkout`
- `actions/setup-java`
- `gradle/actions/setup-gradle` (Gradle caching and wrapper validation)
- `reactivecircus/android-emulator-runner` (emulator on Linux runners with KVM)
- `actions/upload-artifact`

## Impact

- GitHub Actions minutes: free for public repositories, including the Linux runners with KVM used for the emulator.
- Every pull request takes the time of the slower job (expected to be e2e). `WAYOFWORKING.md` tracks CI duration as an open question.
- This change's own pull request is the first one to run CI, so its gate run waits on real checks and does not skip the CI step.
