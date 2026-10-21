## Why

The 8-week validation (#50) needs the app on a real tester's phone through the Google Play Console internal testing track (SESSION0 Phase 1, ADR 0011). Today there is no signed build, no version scheme (versionCode is fixed at 1, no tags, no CHANGELOG although `docs/WAYOFWORKING.md` says versions come from Conventional Commits), no store listing and no privacy policy, which Play requires because the app asks for calendar access. The owner's Play developer account is verified, so this is the next Phase 1 change (#47). Decided by the owner on 2026-10-21: CI builds the signed bundle and the owner uploads it by hand; versions come from release-please; the store listing lives in the repository; the acceptance check is a hand test of the Play build on a physical device.

## What Changes

- **Versioning from Conventional Commits.** release-please keeps a release pull request open on `main` with the next version and the generated `CHANGELOG.md`. Merging it creates the tag and the GitHub release. The version lives in one generated file read by Gradle; `versionCode` is derived from the version, so it always grows and needs no stored state.
- **Signed release bundle in CI.** When a release is created, a job on `main` builds the release Android App Bundle (`bundleRelease`), signs it with the upload key from GitHub Actions secrets (ADR 0014: Play App Signing, agents never see keys), verifies the signature, and uploads the bundle and the R8 mapping file as a workflow artifact. Without the signing environment variables the release build stays unsigned exactly as today, so local builds, `verify` and `scripts/footprint.sh` do not change.
- **Release pull requests get CI.** release-please opens its pull request with an owner-provided token, because pull requests opened with the default workflow token do not trigger CI, and the `main` ruleset requires `verify` and `e2e`.
- **Store listing in the repository.** Title, short and full description in English and Spanish, the 512 px icon, the 1024 x 500 feature graphic and phone screenshots in both languages, laid out in the standard `fastlane/metadata/android/<locale>/` structure (no fastlane tool is added) so a later upload automation can reuse it. Texts and images need the owner's UX approval.
- **Privacy policy** in English and Spanish under `docs/`, linked from the store listing by its GitHub URL: no data leaves the device, no account, no analytics, no ads; calendar access is used only to write the user's own alert events on the device; notifications are local.
- **Release guide** `docs/release.md`: the owner's one-time setup (create the app in Play Console, Play App Signing, generate the upload key, add the secrets and the release token, Data safety, content rating, target audience, ads declaration, app access, tester list) and the steps for each release (merge the release pull request, download the artifact, upload bundle and mapping to the internal track, release notes from the CHANGELOG, install from Play on the device).
- **Rule for generated release pull requests.** `docs/WAYOFWORKING.md` and `AGENTS.md`: the release pull request only contains generated files, so it skips the no-mistakes gate; it still needs green CI and the owner's merge.
- New ADR 0018: versioning and release with release-please, signed in CI and uploaded by hand. It also records that signing moves from Phase 2 into Phase 1 (#47), as ADR 0014's decision is unchanged and only its timing note is outdated.

### New dependencies (approving this proposal approves them)
- GitHub Action `googleapis/release-please-action` (pinned to a full commit SHA).
No new Gradle plugins or libraries: signing and bundling are part of the Android Gradle Plugin; `jarsigner` comes with the JDK.

## Non-goals

- Automatic upload to Play through the Play Developer API (a service account and a new dependency). The first upload of a new app is manual anyway; a follow-up issue is opened to decide on it after a few releases.
- Closed testing with 12 testers for 14 days and production access (Phase 2).
- Running the E2E suite against the release build (decided at the Phase 1 retrospective; this change checks the Play build by hand on a physical device).
- Play Console steps themselves: Data safety, content rating, testers and uploads are owner-only and are documented, not performed.
- Paid release, pricing and store presence beyond what the internal track requires.

## Capabilities

### New Capabilities
- `release-distribution`: versioning from Conventional Commits, the release pull request, the signed release bundle built only for a release, the store listing and privacy policy kept in the repository, and the documented owner steps.

### Modified Capabilities
- `continuous-integration`: the no-secrets rule is narrowed to the pull request checks; the release workflow may use the signing secrets and the release token, only on `main` and never on pull requests.
- `resource-footprint`: the release build configuration still contains no key, but may read the upload key from the environment; without it the release build is unsigned as today.

## Impact

- Depends on: nothing open (#49 merged). Owner steps (secrets, release token, Play Console) are needed before the first release, not before merging this change.
- Touches: `app/build.gradle.kts` (version from file, derived versionCode, signing from environment), the version file and release-please config and manifest at the repository root (new), `.github/workflows/release.yml` (new), `fastlane/metadata/android/` (new), `docs/privacy-policy.md` and its Spanish version (new), `docs/release.md` (new), `docs/adr/0018-*.md` and the ADR index, `docs/WAYOFWORKING.md`, `AGENTS.md`, `README.md` if it lists the release flow. Uses one emulator for the store screenshots: not runnable in parallel with another flow's E2E run.
- Issue: Refs #47. #47 stays open until the owner has installed the first Play build on the physical device and the hand check passes; then it is closed.
