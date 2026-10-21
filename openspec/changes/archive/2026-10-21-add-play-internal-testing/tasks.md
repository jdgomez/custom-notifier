## 1. Version and signing in Gradle

- [x] 1.1 Add `version.txt` (`0.1.0`), `release-please-config.json` and `.release-please-manifest.json` (design decision 1)
- [x] 1.2 Read the version name from `version.txt` and derive the version code with a pure function, with a JVM unit test for "Version code grows" and the rejected inputs (design decision 2)
- [x] 1.3 Add the release signing config from the four environment variables: none set = unsigned as today, some set = build fails naming the missing ones (design decision 5); check by hand with a throwaway local keystore created in the build directory (never committed) that `bundleRelease` is signed with it and that `assembleRelease` without the variables is unsigned; `./gradlew assembleDebug assembleRelease lintAll test` passes and `scripts/footprint.sh` needs no change

## 2. Release workflow

- [x] 2.1 Write `.github/workflows/release.yml` with the `release-please` and `bundle` jobs (design decisions 3 and 4): pinned SHAs, commented permissions, missing-secret check, signature and fingerprint check, version check, artifact, keystore cleanup
- [x] 2.2 Check the workflow audit scenario of `continuous-integration` and the "Pull request run" scenario of `release-distribution` by inspection; run `actionlint` only if it is already installed (no new tool)

## 3. Privacy policy and release guide

- [x] 3.1 Write `docs/privacy-policy.md` and `docs/privacy-policy.es.md` (design decision 7), checked against every permission in `AndroidManifest.xml`
- [x] 3.2 Write `docs/release.md`: one-time owner setup and the per-release steps, including the release token renewal, the upload key backup and reset, the Data safety answers (calendar read/write on device only, no data collected or shared; notifications), the mapping file upload, refreshing screenshots, and the physical-device check of the first release

## 4. Store listing

- [x] 4.1 Draft the English and Spanish title, short and full description (design decision 6), describing only what the app does
- [x] 4.2 Produce the 512 px icon and the 1024 x 500 feature graphic from the launcher icon design
- [x] 4.3 Take the phone screenshots on `cn-api37` in English and Spanish (sample products created through the UI, demo-mode status bar), at least two per language; shut the emulator down afterwards
- [ ] 4.4 (owner) Approve the texts and images as UX; stop and report with the files' paths before going on
- [x] 4.5 Add the test that checks text limits and image sizes ("Listing check" scenario)

## 5. Documentation and ADR

- [x] 5.1 Write ADR 0018 and update the ADR index (design decision 9)
- [x] 5.2 Update `docs/WAYOFWORKING.md` (generated release pull requests, signing moved to Phase 1, versions from release-please) and `AGENTS.md` (Build and run: version file and signing variables; CI: the release workflow and its secrets; Gate: the release pull request rule), and `README.md` only if it describes the release flow
- [x] 5.3 Check the production line count (about 400 at most, tests excluded; workflow and Gradle code count, docs and listing texts do not); if clearly over, stop and report

## 6. Archive

- [x] 6.1 Run `openspec archive add-play-internal-testing`, sync the delta specs into `openspec/specs/` (`release-distribution` new, `continuous-integration` and `resource-footprint` modified), and update the Project state in `AGENTS.md` (#47 merged as code, waiting on the owner's one-time setup and first release from `docs/release.md`; #47 closes after the physical-device check; next #50)
