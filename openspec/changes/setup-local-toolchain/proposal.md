## Why

The development machine has no JDK, no Android SDK and no emulator, so no agent can build, lint or test anything yet. Every other Phase 0 change depends on a working, documented and reproducible local toolchain, and the final project phase requires that this same toolchain can be removed cleanly.

**Depends on:** nothing.
**Touches:** development machine (packages and user home directory), `docs/development-setup.md`.

## What Changes

- Install JDK 17 on the development machine through the distribution package manager.
- Install the Android SDK in the user's home directory from the official command-line tools: platform-tools, emulator, one Android platform, build tools and two x86_64 emulator system images (the latest stable API level and the minimum supported API 26).
- Create two Android Virtual Devices (AVDs) using hardware acceleration (KVM): one at the latest stable API level and one at API 26.
- Add a `docs/development-setup.md` guide describing the exact versions installed, how to reproduce the setup, how to verify it, and how to uninstall it (for the final environment cleanup phase).

## Capabilities

### New Capabilities
- `development-environment`: what a development machine must provide to build and test the app, and how that setup is verified, reproduced and removed.

### Modified Capabilities
- None.

## Non-goals

- Creating the Gradle project or any source code (change `add-android-project-skeleton`).
- Installing Android Studio or any IDE. Agents work from the command line; the owner may install an IDE separately if wanted.
- Configuring CI runners (change `add-ci-workflows`).
- Supporting any machine other than the current development machine (Linux Mint 22.3, x86_64).

## New dependencies (owner approval)

- OpenJDK 17 (`openjdk-17-jdk-headless`, distribution package).
- Android SDK command-line tools, platform-tools, emulator, build-tools, one platform and two `google_apis` x86_64 system images (official Google downloads, SDK license acceptance required).

## Impact

- Machine: about 13 GB of disk in the user's home directory (SDK + two system images + two AVDs); 108 GB free at the time of writing.
- Some steps need `sudo` (package install, and possibly `kvm` group membership), so they are owner steps.
- Repository: one new documentation file only.
