# custom-notifier - agent instructions

Android app that alerts the user before a household consumable runs out. Built with agent engineering: agents write all the code, the owner approves and merges.

## Read first
- `docs/SESSION0.md` - what we are building: problem, scope, glossary, phases. **The glossary is the ubiquitous language: its terms are the type names.**
- `docs/WAYOFWORKING.md` - how we build it: roles, change lifecycle, definition of done, testing strategy, conventions. Changing it requires owner approval.
- `docs/development-setup.md` - the local toolchain (JDK 21, Android SDK, emulators): versions, how to verify or reinstall it, how to remove it.

## Non-negotiables
- Every piece of work is an OpenSpec change: 1 change = 1 branch `change/<name>` = 1 PR = 1 squash commit on `main`, about 400 lines of production code at most.
- Conventional Commits everywhere. Never edit CHANGELOG or generated files by hand.
- Nothing reaches `main` without the no-mistakes gate and green CI. The owner merges; agents never do.
- Stop and escalate (do not improvise) when blocked, when the gate fails 3 times on the same change, or when a decision is not covered by the specs.
- Owner-only: new dependencies, UX approval, secrets, signing keys, store and legal steps.
- Shared agent permissions live in `.claude/settings.json` (changes need owner approval); personal ones go in the gitignored `.claude/settings.local.json`.
- All artifacts in English. No personal references; always "the user".

## Build and run
- Build: `./gradlew assembleDebug` (APK at `app/build/outputs/apk/debug/app-debug.apk`). Modules: `:domain` (pure Kotlin/JVM, no Android) and `:app` (Compose). Versions live only in `gradle/libs.versions.toml`. Room exports its schema to `app/schemas/` (committed; a schema change needs a version bump and a migration). Kotlin warnings fail the build.
- Lint: `./gradlew lintAll` runs Android Lint, ktlint and detekt (also part of `check`); `./gradlew ktlintFormat` auto-fixes formatting. Zero warnings, no baselines. Fix findings at the source; a suppression needs the narrowest scope (one declaration or line) plus a comment saying why, and a global rule deactivation lives only in `.editorconfig`, `config/detekt/detekt.yml` or `lint.xml` with a comment.
- Install and launch on a running emulator: `adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n dev.jdgomez.customnotifier/.MainActivity`. Boot AVDs per `docs/development-setup.md`; run one at a time and shut it down when done.

## Tests
- JVM (unit, Robolectric Compose UI, Roborazzi screenshot verification; no emulator): `./gradlew test`. Reports in `app/build/reports/tests/`, screenshot diffs in `app/build/outputs/roborazzi/` (`*_compare.png`).
- Screenshot references are committed in `app/src/test/screenshots/` and verified pixel-exact. Re-record deliberately with `./gradlew recordRoborazziDebug` and review the PNGs in the diff.
- E2E (instrumented, Orchestrator with clean app state per test): boot one emulator, then `scripts/e2e.sh`. Writes `build/e2e/e2e-run.webm` (whole-run video) and `build/e2e/reports-*`, also on failure; waits for the emulator to finish booting; exits non-zero on failure, when no instrumented test was executed, or unless exactly one emulator (and no other device) is connected. Shut the emulator down afterwards.

## CI
- `.github/workflows/ci.yml` runs on every pull request to `main` and every push to `main`, with two parallel jobs that call the local commands unchanged: `verify` (`./gradlew assembleDebug lintAll test`) and `e2e` (API 37 `google_apis` x86_64 emulator, `scripts/e2e.sh`).
- Artifacts (14-day retention, uploaded also on failure): `verify-reports` (reports, test results, Roborazzi diffs) and `e2e-evidence` (`build/e2e/`: video and reports).
- The job names `verify` and `e2e` are the required status check names in the `main` ruleset (see Gate and `main` protection).
- Actions are pinned to full commit SHAs with a `# vX.Y.Z` comment; the permission is `contents: read` and no secrets are used. A new action is a new dependency (owner approval).

## Gate and `main` protection
- The gate always waits on CI: never use `--skip ci`. `.no-mistakes.yaml` (read by the gate only from `main`) declares `./gradlew lintAll` and `./gradlew test`, has no `ci.no_ci`, and points the gate's PR step at `.github/pull_request_template.md` through `pr.template`. E2E is not run by the gate; CI's `e2e` job provides it.
- The `main` ruleset is defined in `.github/rulesets/main-protection.json`: pull request required (0 approvals, the owner's merge is the review), `verify` and `e2e` required and up to date, no force push, no deletion, linear history, empty bypass list (also for administrators, because agents act through the owner's account). Changing a job name in `ci.yml` means changing this JSON in the same pull request.
- The ruleset is active on `main` (verified in `add-gate-and-repo-protection`: all rules enforced, empty bypass list, direct push rejected with GH013) and the repository allows squash merge only with the PR title as commit title and head branches deleted after merge. Changing the live ruleset or merge settings is an owner-only step (agents never run those write commands); the ruleset JSON stays the source of truth.
- Owner-only reference, for re-applying the protection if the live ruleset or settings are ever lost or the repository is recreated (agents never run these write commands):
  1. Create the ruleset: `gh api -X POST repos/jdgomez/custom-notifier/rulesets --input .github/rulesets/main-protection.json`
  2. Merge settings: `gh api -X PATCH repos/jdgomez/custom-notifier -F allow_squash_merge=true -F allow_merge_commit=false -F allow_rebase_merge=false -F allow_auto_merge=false -F delete_branch_on_merge=true -f squash_merge_commit_title=PR_TITLE -f squash_merge_commit_message=COMMIT_MESSAGES`. `delete_branch_on_merge` deletes only the remote head branch after merge; local branches and worktrees are untouched and the remote branch can be restored from the PR.
  3. Verify the ruleset is active (`gh api repos/jdgomez/custom-notifier/rules/branches/main`, then `gh api repos/jdgomez/custom-notifier/rulesets/<id>`: `enforcement` is `active`, `bypass_actors` is empty).
- Emergency: the owner can temporarily set the ruleset enforcement to `disabled` in the GitHub UI (Settings, Rules), make the fix, and re-enable it right away. Emergency changes otherwise also go through a pull request. Agents never disable the ruleset.

## Development practices and ADRs
- SDD with OpenSpec: behavior is specified before code ([0012](docs/adr/0012-development-practices.md)). Spec scenarios use Given / When / Then; no Gherkin tooling.
- Tactical DDD: glossary terms in `docs/SESSION0.md` are the type names; domain concepts are dedicated types, not bare primitives; depletion logic lives in the domain, never in a ViewModel.
- Ports and adapters: `:domain` is pure Kotlin with no Android dependencies; persistence, notifications and calendar are adapters.
- Tests must exercise real behavior; a bug fix needs a test that fails before the fix.
- Decisions are recorded in `docs/adr/` (index: [`docs/adr/README.md`](docs/adr/README.md)). A significant technical decision (new architectural pattern, new module, new dependency with lasting impact, or reversal of an existing ADR) needs an ADR in the same change. Accepted ADRs are never rewritten: add a new ADR that supersedes the old one, set the old status to `Superseded by NNNN`, and update the index.

## Project state
Phase 0 (foundation) is complete and Phase 1 has started: the local toolchain is installed and the Gradle project exists (`add-android-project-skeleton`: placeholder screen, verified on `cn-api37` and `cn-api26`). Lint is set up (`add-lint-setup`); test setup exists (`add-test-infrastructure`); CI is defined in `.github/workflows/ci.yml` (`add-ci-workflows`); the gate config exists and the `main` ruleset is active (`add-gate-and-repo-protection`). Phase 1: the product model with exact stock estimation exists in `:domain` (`add-product-stock-domain`, done); the rule, depletion date and next alert exist in `:domain` (`add-depletion-and-alert-domain`, done); products are stored with Room 3 behind the `ProductRepository` port, wired by a manual `AppContainer` (`add-room-persistence`, ADR 0015, done); the main screen is the product list with the fixed light/dark theme, a ViewModel recomputing every minute while visible, and `AppContainer` exposing the clock and zone (`add-product-list-ui`, done); products can be created from a form reached by navigation with AndroidX Navigation 3, back stack in memory (`add-product-create-ui`, ADR 0016, done). Planned stack in `README.md`. Tapping a product row opens the same form to edit it (stock not editable) with a confirmed delete (`add-product-edit-delete-ui`, done).

## GitHub
Milestones per phase; issues per change (labels `change`, `spike`, `adr`, `parallelizable`, `blocked`, `needs-human`); public board at https://github.com/users/jdgomez/projects/1. PRs close their issue with `Closes #N`.
