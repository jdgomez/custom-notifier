# 0018. Versioning and release with release-please, signed in CI, uploaded by hand

Date: 2026-10-21
Status: Accepted

## Context

The 8-week validation needs a signed build on a real tester's phone through the Play internal testing track ([0011](0011-internal-testing-distribution.md)). [0014](0014-play-app-signing.md) decided on Play App Signing with the upload key as a GitHub Actions secret, and noted that signing happens in Phase 2; it moves into Phase 1 (#47) because the tester needs the build now. The decision of 0014 is unchanged and its timing note is outdated; it is not edited.

## Decision

- **Versions from Conventional Commits with release-please** (`googleapis/release-please-action`, release type `simple`, `bump-minor-pre-major`). It keeps one release pull request open on `main`; merging it creates the tag and the GitHub release. The version lives in the generated `version.txt`, with `CHANGELOG.md` and `.release-please-manifest.json`; nobody edits them by hand.
- **versionCode derived from the version name:** `MAJOR * 1_000_000 + MINOR * 1_000 + PATCH`, with MINOR and PATCH below 1000 and MAJOR at most 2099 (Play's limit is 2_100_000_000). It grows with every later version and needs no stored state. A build with any other `version.txt` content fails.
- **Signed bundle built in CI, only for a release:** `.github/workflows/release.yml` runs on `main` only. After the release is created, one job builds `bundleRelease` with the upload key from GitHub Actions secrets, checks the signature and that the certificate fingerprint equals the public variable `UPLOAD_CERT_SHA256`, checks the version, and keeps `app-release.aab` and `mapping.txt` as an artifact. Gradle reads the key only from four environment variables; with none set the release build stays unsigned, with some set the build fails.
- **Release token:** the release pull request is opened with a fine-grained personal access token (`RELEASE_PLEASE_TOKEN`), because pull requests opened with the default token do not trigger `verify` and `e2e`, which the `main` ruleset requires.
- **The owner uploads the bundle to Play by hand** ([docs/release.md](../release.md)).
- **The generated release pull request skips the no-mistakes gate:** it only changes `version.txt`, `CHANGELOG.md` and `.release-please-manifest.json`; Brain checks that diff, CI must be green, the owner merges.

## Alternatives considered

- **Manual `v*` tag with versionCode from the run number:** no CHANGELOG and a version that depends on CI state. Rejected by the owner.
- **release-please Gradle updater with a marker in `app/build.gradle.kts`:** puts generated text in hand-written build code. A plain file is simpler.
- **Automatic upload through the Play Developer API:** needs a service account and a new dependency, and the first upload of a new app is manual anyway. To be reconsidered after a few releases.
- **Bundle job on the tag push:** pushes made with a workflow token do not start other workflows; one workflow keeps the release in one run.
- **GitHub environment with required reviewers for the signing job:** one more click per release with little gain while only the owner can merge to `main`; can be added later.
- **A GitHub App instead of the personal access token:** sturdier but more owner setup for one repository; mentioned as the upgrade path.

## Consequences

- The key exists only as repository secrets and is reachable only by the `bundle` job of the release workflow on `main`; no pull request run uses a secret. The release workflow is the only one allowed to reference `secrets.`.
- The release token expires and must be renewed (documented); until then every push to `main` runs the `release-please` job.
- A lost upload key is handled by Play's upload key reset (documented).
- `docs/WAYOFWORKING.md` and `AGENTS.md` describe the release pull request rule and the signing timing.
