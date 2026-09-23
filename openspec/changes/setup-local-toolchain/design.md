## Context

Machine state checked on 2026-09-23: Linux Mint 22.3 (Ubuntu 24.04 base), x86_64, 16 cores, 31 GB RAM, 108 GB free disk. No `java`, no `~/Android`, no `~/.gradle`. `/dev/kvm` exists and is currently readable and writable by the user through a login-session ACL, not through `kvm` group membership. `openjdk-17-jdk-headless` is available from the distribution repositories.

## Goals / Non-Goals

**Goals:**
- A command-line-only toolchain that agents in Herdr panes can use without interactive prompts.
- Exact versions recorded, so CI (change `add-ci-workflows`) can mirror them.
- Clean removal later, as `SESSION0.md` requires for the final phase.

**Non-Goals:**
- Gradle installation: the Gradle wrapper arrives with the project skeleton and downloads Gradle itself.
- Choosing `compileSdk` / `targetSdk` for the app: that belongs to `add-android-project-skeleton`. This change installs the platform that change will use.

## Decisions

### JDK from the distribution package, not a manual tarball or SDKMAN
`apt install openjdk-17-jdk-headless`. It receives security updates with the rest of the system and uninstalls with one command.
- Alternative: Temurin tarball or SDKMAN in the home directory. These need no `sudo`, but they get no automatic security updates and add a second tool to manage. Rejected.
- Headless variant: no desktop Java UI is needed.

### Android SDK in `~/Android/Sdk` from the official command-line tools
Download the official `commandlinetools-linux-*.zip`, verify its SHA-256 against the value published on the Android developer downloads page, and unpack it to `~/Android/Sdk/cmdline-tools/latest`. Then install with `sdkmanager`:
- `platform-tools`, `emulator`
- `platforms;android-<N>` and `build-tools;<N>.x.y`, where N is the latest stable API level at install time
- `system-images;android-<N>;google_apis;x86_64`
- `system-images;android-26;google_apis;x86_64`, so the minimum supported version (`SESSION0.md`) can be verified locally. Only the system image is needed: apps compile against platform N.

`~/Android/Sdk` is the standard location, and most tools find it without extra configuration. No `sudo` is needed, and removal is `rm -rf`.
- Alternative: install Android Studio just to get the SDK. Rejected: it is a heavy IDE that agents do not use.

### Image type: `google_apis`, not `google_apis_playstore`
Calendar and notification E2E tests need the on-device Calendar Provider, which the `google_apis` images include. The Play Store images restrict root and `adb` operations that tests may need. CI can use the same image type.

### Environment variables in `~/.profile`
Set `ANDROID_HOME` and add `cmdline-tools/latest/bin`, `platform-tools` and `emulator` to the `PATH` in `~/.profile`, so login shells and Herdr panes all see them. `JAVA_HOME` is only set if Gradle cannot resolve Java from the `PATH`.

### AVD definition
Create two AVDs on a common phone profile (for example `pixel_6`), one per installed system image: `cn-api<N>` (the default for development and E2E) and `cn-api26` (the minimum supported version). Record the exact `avdmanager` commands in the guide.

### KVM access through the `kvm` group
The current access comes from a login-session ACL. That ACL may not apply to processes outside the graphical seat session, and it goes away when the user logs out. Adding the user to the `kvm` group gives permanent access (owner step, `sudo usermod -aG kvm $USER`, then log in again).

## Risks / Trade-offs

- [SDK license prompts block unattended agents] → Accept all licenses once during setup with `yes | sdkmanager --licenses`, as an owner-supervised step.
- [The latest API level has a known emulator issue] → Fall back to the previous stable API level and record the reason in the guide.
- [Disk use grows with more system images] → Install only the two images above now. Add more only when a change needs them.
- [The emulator competes with running agents for CPU/RAM (a known concern on this machine)] → Use headless boot with a bounded AVD RAM (2 GB) and shut the emulator down after verification.

## Migration Plan

Nothing to migrate. Rollback = the uninstall section of the guide.
