## 1. Toolchain

- [x] 1.1 Set `jvmToolchain(21)` in `:app` and `:domain`
- [x] 1.2 Run `./gradlew clean assembleDebug lintAll check` on JDK 21 with no warnings; fix any finding at the source

## 2. Robolectric on the target SDK

- [x] 2.1 Remove the SDK 35 pin (and `robolectric.properties` if it becomes empty) and the related `AGENTS.md` line
- [x] 2.2 Run `./gradlew test` and confirm from the test reports that Robolectric ran on the app's target SDK
- [x] 2.3 If screenshot verification fails only because rendering changed, re-record the reference and keep the old and new images for the PR description (owner approves the image change)

## 3. E2E

- [x] 3.1 Run `scripts/e2e.sh` on `cn-api37`, then on `cn-api26`, one emulator at a time, shutting each down afterwards

## 4. Docs and planning

- [x] 4.1 Update `docs/development-setup.md` to JDK 21 (intro, versions table, install, verify, remove; the removal names only JDK 21 packages)
- [x] 4.2 Add to `docs/development-setup.md` the one-time JDK 17 removal step for machines that still have it (owner)
- [x] 4.3 Update the JDK mention in `AGENTS.md` and the development machine line in `docs/WAYOFWORKING.md`
- [x] 4.4 Update `openspec/changes/add-ci-workflows` (design "Java distribution in CI" and task 1.3) to Temurin 21

## 5. Commit and gate

- [x] 5.1 Commit on `change/move-toolchain-to-jdk-21` with Conventional Commits
- [ ] 5.2 Run the no-mistakes gate with `--skip ci`

## 6. After merge

- [ ] 6.1 Remove JDK 17 with the documented command (owner)
- [ ] 6.2 Re-run the Java version check and `./gradlew check` on `main` to confirm JDK 21 is the only JDK and the build passes
