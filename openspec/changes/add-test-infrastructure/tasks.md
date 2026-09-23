## 1. JVM unit tests

- [ ] 1.1 Add JUnit 4 and `kotlin-test` to the catalog and to `:domain` and `:app`
- [ ] 1.2 Prove the `:domain` setup with a throwaway test (not committed); record the output in the PR description

## 2. Robolectric UI tests

- [ ] 2.1 Add Robolectric and Compose UI test dependencies; enable Android resources in unit tests and native graphics
- [ ] 2.2 Add a UI test asserting the placeholder screen displays the app name

## 3. Screenshot tests

- [ ] 3.1 Add the Roborazzi plugin and libraries; set verify-by-default and the reference directory
- [ ] 3.2 Add a screenshot test for the placeholder screen and record its reference image
- [ ] 3.3 Prove that a one-pixel change fails `./gradlew test` and produces a comparison image (scratch, not committed)

## 4. E2E tests

- [ ] 4.1 Configure the instrumentation runner, the Orchestrator and `clearPackageData`; add AndroidX Test, Compose UI test and UI Automator to `androidTest`
- [ ] 4.2 Add the launch E2E test (UI Automator launch, Compose assertion on the main screen)
- [ ] 4.3 Write `scripts/e2e.sh` (device check, recording, test run, always-stop recording, reports copied, exit code preserved)
- [ ] 4.4 Run it on the local emulator; confirm the video plays and the report exists; attach a frame or the video to the PR
- [ ] 4.5 Prove a failing E2E test still leaves the video and report (scratch, not committed)

## 5. Docs and gate

- [ ] 5.1 Document the test commands and output locations in `AGENTS.md`
- [ ] 5.2 Commit on `change/add-test-infrastructure` with Conventional Commits
- [ ] 5.3 Run the no-mistakes gate with `--skip ci` (no CI on `main` yet)
