# custom-notifier - agent instructions

Android app that alerts the user before a household consumable runs out. Built with agent engineering: agents write all the code, the owner approves and merges.

## Read first
- `docs/SESSION0.md` - what we are building: problem, scope, glossary, phases. **The glossary is the ubiquitous language: its terms are the type names.**
- `docs/WAYOFWORKING.md` - how we build it: roles, change lifecycle, definition of done, testing strategy, conventions. Changing it requires owner approval.
- `docs/development-setup.md` - the local toolchain (JDK 17, Android SDK, emulators): versions, how to verify or reinstall it, how to remove it.

## Non-negotiables
- Every piece of work is an OpenSpec change: 1 change = 1 branch `change/<name>` = 1 PR = 1 squash commit on `main`, about 400 lines of production code at most.
- Conventional Commits everywhere. Never edit CHANGELOG or generated files by hand.
- Nothing reaches `main` without the no-mistakes gate and green CI. The owner merges; agents never do.
- Stop and escalate (do not improvise) when blocked, when the gate fails 3 times on the same change, or when a decision is not covered by the specs.
- Owner-only: new dependencies, UX approval, secrets, signing keys, store and legal steps.
- Shared agent permissions live in `.claude/settings.json` (changes need owner approval); personal ones go in the gitignored `.claude/settings.local.json`.
- All artifacts in English. No personal references; always "the user".

## Build and run
- Build: `./gradlew assembleDebug` (APK at `app/build/outputs/apk/debug/app-debug.apk`). Modules: `:domain` (pure Kotlin/JVM, no Android) and `:app` (Compose). Versions live only in `gradle/libs.versions.toml`. Kotlin warnings fail the build.
- Install and launch on a running emulator: `adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n dev.jdgomez.customnotifier/.MainActivity`. Boot AVDs per `docs/development-setup.md`; run one at a time and shut it down when done.

## Project state
Phase 0 (foundation) is in progress: the local toolchain is installed and the Gradle project exists (`add-android-project-skeleton`: placeholder screen, verified on `cn-api37` and `cn-api26`). There is no CI, lint or test setup yet. Planned stack in `README.md`.

## GitHub
Milestones per phase; issues per change (labels `change`, `spike`, `adr`, `parallelizable`, `blocked`, `needs-human`); public board at https://github.com/users/jdgomez/projects/1. PRs close their issue with `Closes #N`.
