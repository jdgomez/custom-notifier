## 1. Android Lint

- [ ] 1.1 Configure `lint {}` in `:app` (warnings as errors, abort on error, check dependencies, no baseline)
- [ ] 1.2 Run `./gradlew :app:lint` and fix any finding in the skeleton at the source

## 2. ktlint

- [ ] 2.1 Add the ktlint Gradle plugin to the catalog and apply it to all Kotlin modules
- [ ] 2.2 Add `.editorconfig` with the ktlint style and the Composable naming exception
- [ ] 2.3 Run `ktlintFormat` then `ktlintCheck`; the result is clean

## 3. detekt

- [ ] 3.1 Add the detekt Gradle plugin and the Compose rules to the catalog, compatible with the project's Kotlin version
- [ ] 3.2 Add `config/detekt/detekt.yml` (overrides only, commented), `maxIssues: 0`, warnings as errors, no baseline
- [ ] 3.3 Run `detekt`; the result is clean

## 4. Wiring and proof

- [ ] 4.1 Register the root `lintAll` task and make `check` depend on the analyzers
- [ ] 4.2 Prove each analyzer fails the build: introduce one violation per analyzer on a scratch copy, confirm the command fails and names the rule, then discard the scratch copy (record the evidence in the PR description, do not commit the violations)

## 5. Docs and gate

- [ ] 5.1 Add `./gradlew lintAll` and `./gradlew ktlintFormat` plus the suppression rule to `AGENTS.md`
- [ ] 5.2 Commit on `change/add-lint-setup` with Conventional Commits
- [ ] 5.3 Run the no-mistakes gate with `--skip ci` (no CI on `main` yet)
