## Context

By the time this change starts, the repository has:
- the lint command (`./gradlew lintAll`)
- the JVM test command (`./gradlew test`, which also verifies screenshots)
- the E2E entry point (`scripts/e2e.sh`, which needs a booted emulator and writes to `build/e2e/`)

The repository is public, so hosted Linux runners are free and expose `/dev/kvm` once a udev rule grants access. `no-mistakes ci-workflow` can generate a basic `ci.yml` from `.no-mistakes.yaml` commands.

## Goals / Non-Goals

**Goals:**
- CI runs exactly the local commands. There is no CI-only test logic.
- Evidence is always uploaded, even on failure.
- Stable check names for branch protection.

**Non-Goals:**
- Matrix builds across API levels. One emulator API level for now; add the minimum API level (26) to the matrix if Phase 1 finds version-specific behavior (notifications and exact alarms are likely candidates).

## Decisions

### Hand-written workflow instead of `no-mistakes ci-workflow`
The generator mirrors the gate's commands, but it does not produce the emulator job or the artifact uploads. Editing a generated file by hand conflicts with the rule against modifying generated files. So `ci.yml` is hand-written and owned by the project.

### One workflow `ci.yml`, two parallel jobs `verify` and `e2e`
The jobs run in parallel, so total time is the slower job, not the sum of both. Job names are the check names used by branch protection, and must not be renamed without updating protection in the same change.
- Alternative: have e2e wait for verify (`needs: verify`) to save minutes when lint fails. Rejected: minutes are free, and faster feedback matters more.

### `verify` job
Steps: checkout → setup-java (Temurin 21) → setup-gradle (with wrapper validation) → `./gradlew assembleDebug lintAll test`. Always upload `**/build/reports/`, `**/build/test-results/` and `**/build/outputs/roborazzi/`.

### `e2e` job
Steps: enable KVM with the documented udev rule → checkout → setup-java → setup-gradle → `reactivecircus/android-emulator-runner`, using the same API level and `google_apis` x86_64 image as the local `cn-api<N>` AVD, with `script: ./scripts/e2e.sh`. The AVD gets the same data partition size as the local AVDs (`disk-size: 10G`): the runner's default is too small and the APK install fails with "not enough space" before any test runs. The APKs are built in a separate step before the emulator boots (`./gradlew assembleDebug assembleDebugAndroidTest`), so `connectedDebugAndroidTest` inside `scripts/e2e.sh` only installs and runs. Reason: a proof run compiled for about 5 minutes with the emulator already booted on a 4-vCPU runner (2 cores to the emulator), then the install failed with "Cannot access system provider 'settings' before system providers are installed", consistent with `system_server` restarting under CPU starvation (unconfirmed, no logcat). `ram-size` stays at 2048M like the local AVD. The runner image ships Android command-line tools 12.0, whose `avdmanager` writes `target=android-0` for `android-37.0` AVDs; the emulator then crash-loops SurfaceFlinger and `system_server` (the `Failed to find ColorBuffer` errors and vanishing `settings`/`package` services seen in proof runs; ReactiveCircus/android-emulator-runner issue #482). The runner action only installs its own tools when `$ANDROID_HOME/cmdline-tools` does not exist, so a step before it installs `cmdline-tools;22.0` (the version used locally) as `cmdline-tools/latest`, and a `pre-emulator-launch-script` fails the job unless the AVD has `target=android-37.0`. Always upload `build/e2e/`. The emulator cold-boots on every run: no AVD snapshot cache, because that needs `actions/cache` (not an approved action) and a cold boot is deterministic, at the cost of a longer boot (about 30 s locally, longer on hosted runners).

### Triggers and concurrency
`on: pull_request` (branches `main`) and `push` (branches `main`). `concurrency: group: ci-${{ github.ref }}`, with `cancel-in-progress: true` only for pull requests.

### Security
Top-level `permissions: contents: read`. Actions are pinned by SHA, with `# vX.Y.Z` comments so updates stay readable. No secrets. Pull requests from forks get the same read-only token by default.

### Java distribution in CI: Temurin 21
The local JDK is the distribution's OpenJDK 21. Temurin 21 in CI is the same major version from a well-maintained build. A difference in patch versions is acceptable.

## Risks / Trade-offs

- [Emulator boots on hosted runners are flaky or slow] → Cold boot every run (no snapshot cache; deterministic, but slower), a boot timeout, and emulator options from the action's documentation (`-no-window -gpu swiftshader_indirect -noaudio -no-boot-anim`). Flakiness is a defect to fix, not to retry around: a retry needs an issue explaining it.
- [Screenshot references recorded locally differ from CI rendering] → Covered in `add-test-infrastructure`. If a difference appears, CI is the source of truth and the re-record procedure is documented.
- [Artifact retention fills storage] → Set `retention-days: 14` on uploads. PR evidence is only needed until merge.
