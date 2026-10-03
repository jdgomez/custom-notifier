## 1. Android Lint

- [x] 1.1 Configure `lint {}` in `:app` (warnings as errors, abort on error, check dependencies, no baseline)
- [x] 1.2 Run `./gradlew :app:lint` and fix any finding in the skeleton at the source

## 2. ktlint

- [x] 2.1 Add the ktlint Gradle plugin to the catalog and apply it to all Kotlin modules
- [x] 2.2 Add `.editorconfig` with the ktlint style and the Composable naming exception
- [x] 2.3 Run `ktlintFormat` then `ktlintCheck`; the result is clean

## 3. detekt

- [x] 3.1 Add the detekt Gradle plugin and the Compose rules to the catalog, compatible with the project's Kotlin version
- [x] 3.2 Add `config/detekt/detekt.yml` (overrides only, commented), `maxIssues: 0`, warnings as errors, no baseline
- [x] 3.3 Run `detekt`; the result is clean

## 4. Wiring and proof

- [x] 4.1 Register the root `lintAll` task and make `check` depend on the analyzers
- [x] 4.2 Prove each analyzer fails the build: introduce one violation per analyzer on a scratch copy, confirm the command fails and names the rule, then discard the scratch copy (record the evidence in the PR description, do not commit the violations)

## 5. Docs and gate

- [x] 5.1 Add `./gradlew lintAll` and `./gradlew ktlintFormat` plus the suppression rule to `AGENTS.md`
- [x] 5.2 Commit on `change/add-lint-setup` with Conventional Commits
- [ ] 5.3 Run the no-mistakes gate with `--skip ci` (no CI on `main` yet)
