## 1. Java

- [ ] 1.1 (owner) Run `sudo apt install openjdk-17-jdk-headless`; the executor prepares the exact command and waits for confirmation
- [ ] 1.2 Verify `java -version` and `javac -version` report 17 in a new login shell

## 2. Android SDK

- [ ] 2.1 Download the official command-line tools zip, verify its SHA-256 against the published value, and unpack it to `~/Android/Sdk/cmdline-tools/latest`
- [ ] 2.2 Add `ANDROID_HOME` and the SDK tool directories to `~/.profile`; verify in a new login shell
- [ ] 2.3 (owner) Accept the SDK licenses (`yes | sdkmanager --licenses`) after reviewing them
- [ ] 2.4 Install platform-tools, emulator, the latest stable platform, the matching build-tools, and the `google_apis` x86_64 system images for the latest stable API level and API 26
- [ ] 2.5 Verify `sdkmanager --list_installed`, `adb version` and `emulator -version`

## 3. Emulator

- [ ] 3.1 (owner) Add the user to the `kvm` group (`sudo usermod -aG kvm $USER`) and log in again
- [ ] 3.2 Verify `emulator -accel-check` reports KVM usable
- [ ] 3.3 Create the AVDs `cn-api<N>` and `cn-api26`, each with 2 GB RAM
- [ ] 3.4 Boot each AVD headless, confirm `sys.boot_completed` is `1` within 3 minutes, then shut it down

## 4. Documentation

- [ ] 4.1 Write `docs/development-setup.md`: exact versions, install steps, verification steps, uninstall steps
- [ ] 4.2 Link the guide from `AGENTS.md` ("Read first") and `README.md`

## 5. Gate

- [ ] 5.1 Commit on `change/setup-local-toolchain` with Conventional Commits
- [ ] 5.2 Run the no-mistakes gate with `--skip ci`: no CI workflow exists on `main` until `add-ci-workflows` is merged, and without the skip the CI step waits forever
