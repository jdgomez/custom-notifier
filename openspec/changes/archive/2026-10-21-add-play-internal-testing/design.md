## Context

See proposal.md - Why. Current state: `app/build.gradle.kts` has `versionCode = 1`, `versionName = "0.1.0"` and a shrunk, unsigned release build type (ADR 0014 note: "No signing config"). `.github/workflows/ci.yml` runs `verify` and `e2e` on pull requests and on `main`, with `contents: read` and no secrets. The `main` ruleset requires `verify` and `e2e`, up to date, linear history, squash merge, no bypass. The repository is public. The app id `dev.jdgomez.customnotifier` becomes permanent with the first upload.

## Goals / Non-Goals

**Goals:** a release is one owner action (merge the release pull request) followed by a documented manual upload; signing secrets are reachable by as little code as possible; local builds and the footprint script are unaffected.

**Non-Goals:** see proposal.md - Non-goals. Also no GitHub environment with required reviewers (one more click per release with little gain while only the owner can merge to `main`; can be added later without changing the specs).

## Decisions

1. **release-please, release type `simple`.** Configuration in `release-please-config.json` and `.release-please-manifest.json` at the root, starting from the current version `0.1.0` with `bump-minor-pre-major` (a `feat` before 1.0.0 bumps the minor, a breaking change does not jump to 1.0.0). The `simple` type keeps the version in `version.txt` and writes `CHANGELOG.md`, both generated. The changelog starts from the full history (the first entry lists everything up to the first release); `bootstrap-sha` is not set.
   - Alternative: a manual `v*` tag with versionCode from the run number. Rejected by the owner: no CHANGELOG, and the version would depend on CI state.
   - Alternative: the Gradle-file updater with an `x-release-please-version` marker in `app/build.gradle.kts`. Rejected: a plain file keeps the generated part out of hand-written build code.

2. **versionCode derived from the version name.** Gradle reads `version.txt` (trimmed, strict `MAJOR.MINOR.PATCH`, the build fails otherwise) and computes `versionCode = MAJOR * 1_000_000 + MINOR * 1_000 + PATCH`, with MINOR and PATCH each required to stay below 1000 (the build fails otherwise). This is monotonic for every later semver version, has no state, and stays far below Play's limit (2_100_000_000) up to major 2099. The computation is a small pure function with a JVM unit test covering the "Version code grows" scenario and the rejected inputs.

3. **One workflow `.github/workflows/release.yml` on `push` to `main` only**, with two jobs:
   - `release-please`: runs the action with the token secret `RELEASE_PLEASE_TOKEN`; outputs `release_created`, `tag_name`. Permissions: `contents: write` and `pull-requests: write` with a comment (the token does the writes, but the default token still needs them for the action's reads and the release).
   - `bundle`: `needs: release-please`, `if: release_created`, `permissions: contents: read`. Checks out the tag, sets up Java 21 and Gradle exactly like `verify` (same pinned SHAs, wrapper validation), decodes the base64 keystore secret into `$RUNNER_TEMP`, runs `./gradlew bundleRelease` with the signing environment variables, verifies with `jarsigner -verify -strict` and checks that the signer certificate's SHA-256 fingerprint equals the repository variable `UPLOAD_CERT_SHA256` (a public value; this proves the right key was used), checks version name and code inside the bundle against the tag, and uploads `app-release.aab` and `mapping.txt` as the artifact `release-<version>` (90-day retention). The keystore file is deleted in an `always()` step. The bundle is not attached to the public GitHub release: the artifact is enough for a manual upload and does not publish the signed bundle.
   - It runs the same `concurrency` pattern as CI without cancelling (never cancel a release).
   - Alternative: trigger the bundle job on the tag push. Rejected: pushes made with a workflow token do not start other workflows, and a single workflow keeps the release in one run.

4. **The release token.** Pull requests opened with the default `GITHUB_TOKEN` do not trigger `pull_request` workflows, so the release pull request would never get `verify` and `e2e` and could not be merged under the ruleset. The owner creates a fine-grained personal access token for this repository only, with Contents and Pull requests read and write, stored as `RELEASE_PLEASE_TOKEN`, with an expiry and a renewal note in `docs/release.md`. A GitHub App is more robust but is more owner setup for one repository; the guide mentions it as the upgrade path.

5. **Signing configuration from the environment.** `app/build.gradle.kts` defines a release signing config only when all four variables are set: `CN_UPLOAD_KEYSTORE` (file path), `CN_UPLOAD_KEYSTORE_PASSWORD`, `CN_UPLOAD_KEY_ALIAS`, `CN_UPLOAD_KEY_PASSWORD`. None set: the release build stays unsigned as today. Some but not all set: the build fails naming the missing ones (a half-configured release must not silently produce an unsigned bundle). The secrets are `UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD`; the bundle job fails with the missing secret's name before building if one is empty.

6. **Store listing layout.** `fastlane/metadata/android/en-US/` and `es-ES/` with `title.txt`, `short_description.txt`, `full_description.txt`, `images/phoneScreenshots/*.png`; `images/icon.png` (512 x 512, from the launcher icon design) and `images/featureGraphic.png` (1024 x 500) under `en-US/` only, as Play uses them for every language. A JVM test (or a check in the existing test setup) asserts the text limits and image sizes of the "Listing check" scenario so a later edit cannot break them. Screenshots come from `cn-api37` with sample products created through the UI, in both languages, 1080 x 2400 or the emulator's native size, status bar clean (demo mode). Title: "Custom Notifier" in both languages unless the owner chooses otherwise during the UX approval.

7. **Privacy policy.** `docs/privacy-policy.md` (English) and `docs/privacy-policy.es.md` (Spanish), linked from the listing by `https://github.com/jdgomez/custom-notifier/blob/main/docs/privacy-policy.md`. Contact: the repository's GitHub issues page, so no personal address appears in the repository (the owner's developer email is shown by Play itself). It lists the manifest permissions that touch user data and is checked against `AndroidManifest.xml` in the review.

8. **Generated release pull requests skip the gate.** The release pull request changes only `version.txt`, `CHANGELOG.md` and `.release-please-manifest.json`. `docs/WAYOFWORKING.md` and `AGENTS.md` state: it does not go through the no-mistakes gate, Brain checks that its diff touches only those three files, CI must be green, and the owner merges it. Any other change goes through a normal change.

9. **ADR 0018** "Versioning and release with release-please, signed in CI, uploaded by hand": decisions 1 to 5 and 8, and the note that signing moves into Phase 1 for #47 (ADR 0014's decision stands; its Phase 2 timing note is outdated and is not edited). Index updated. `docs/WAYOFWORKING.md`: the Phase 2 owner-only steps line and the Constraints line move the signing setup to Phase 1.

## Risks / Trade-offs

- [The release token can push to the repository] → fine-grained, this repository only, two permissions, expiry; it cannot bypass the ruleset (empty bypass list), so it still cannot write to `main` directly.
- [The first release PR's changelog is long] → accepted once; later entries are per release.
- [Wrong key used (for example a new key after a loss)] → the fingerprint check fails the run; key loss is handled by Play's upload key reset, documented in the guide.
- [Release PR goes stale against `main` (ruleset requires up to date)] → release-please rebases its pull request on each push to `main`.
- [Store screenshots drift from the app] → they are refreshed in any change that visibly changes the main screens; noted in `docs/release.md`.
- [A version name with MINOR or PATCH of 1000 or more] → build fails loudly; practically unreachable.

## Migration Plan

Merging this change creates nothing on Play. Then the owner does the one-time setup from `docs/release.md`; the next push to `main` opens the first release pull request (0.2.0); merging it produces the first signed bundle; the owner uploads it to the internal track and runs the physical-device check; then #47 is closed. Rollback: delete the workflow; the version file and changelog stay valid.

## Open Questions

- Final store texts and images: drafted by the executor, approved by the owner as UX before the pull request is opened (task 4.4).
