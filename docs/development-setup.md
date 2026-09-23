# Development setup

How the local toolchain is installed, verified and removed. It gives agents and the owner what they need to build, lint and test the app from the command line: JDK 17, the Android SDK, and two hardware-accelerated emulators. Android Studio is not required.

Steps marked **Owner step** need `sudo` or a license decision. Agents never run them; they prepare the command and wait for the owner.

## Machine assumptions

- Linux Mint 22.3 (Ubuntu 24.04 base), x86_64. No other machine is supported.
- `/dev/kvm` exists and the user can read and write it.
- About 11 GB of free disk in the user's home directory (SDK about 9 GB, AVDs about 2 GB).
- Everything except the JDK and the `kvm` group lives under the user's home directory and needs no `sudo`.

## Installed versions

Recorded on 2026-09-23. The latest stable API level at that date is **37** (Android 17).

| Component | Package / file | Version |
|---|---|---|
| JDK | apt `openjdk-17-jdk-headless` | 17.0.20.1+1-1~24.04 |
| Command-line tools | `commandlinetools-linux-15859902_latest.zip` | 22.0 (build 15859902) |
| Platform-tools | `platform-tools` | 37.0.1 |
| Emulator | `emulator` | 37.1.11 |
| Platform | `platforms;android-37.0` | revision 2 |
| Build-tools | `build-tools;37.0.0` | 37.0.0 |
| System image, latest | `system-images;android-37.0;google_apis;x86_64` | revision 6 |
| System image, minimum | `system-images;android-26;google_apis;x86_64` | revision 16 |

Command-line tools zip:

- URL: `https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip`
- SHA-256: `4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583`
- Source of the hash: the Linux row of the "Command line tools only" table on <https://developer.android.com/studio#command-line-tools-only>.

Choices behind the table:

- **API 37 as latest.** `platforms;android-37.0` and `system-images;android-37.0;google_apis;x86_64` are the newest stable API level with a plain `google_apis` x86_64 image. The 37.1 and 37.2 minor releases only ship 16 KB page size images, so they are not used. Preview, beta and extension variants are excluded.
- **API 26 as minimum.** Only the system image is installed. The app compiles against platform 37.
- **`google_apis`, not `google_apis_playstore`.** Calendar and notification tests need the on-device Calendar Provider and unrestricted `adb`. The Play Store images restrict both.
- **Reproducibility.** The command-line tools zip is pinned by URL and hash. `sdkmanager` installs the current revision of every other package, so a later reinstall may yield newer revisions than the table. Compare against the table and record any difference.

## Install

### 1. JDK 17

**Owner step:**

```bash
sudo apt install openjdk-17-jdk-headless
```

`JAVA_HOME` is not set: Gradle and the SDK tools resolve Java from the `PATH`. Set it only if that stops working.

### 2. Android command-line tools

```bash
mkdir -p "$HOME/Android/Sdk/cmdline-tools"
cd "$(mktemp -d)"
curl -fL -O https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip
echo "4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583  commandlinetools-linux-15859902_latest.zip" | sha256sum -c -
unzip -q commandlinetools-linux-15859902_latest.zip
mv cmdline-tools "$HOME/Android/Sdk/cmdline-tools/latest"
```

`sha256sum -c` must print `OK`. Stop if it does not. The zip contains a top-level `cmdline-tools/` directory, which becomes `latest/`. Delete the temporary directory afterwards.

### 3. Environment variables

Append this block to `~/.profile`, so login shells and agent panes see the SDK. The markers let the uninstall step remove exactly this block.

```bash
# >>> custom-notifier android sdk >>>
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
# <<< custom-notifier android sdk <<<
```

Start a new login shell (or `source ~/.profile`) so the variables apply.

### 4. SDK licenses

**Owner step:** read and accept the licenses once:

```bash
yes | sdkmanager --licenses
```

Agents never accept licenses. If an install stops on a license prompt, escalate to the owner.

### 5. SDK packages

```bash
sdkmanager --install \
  "platform-tools" \
  "emulator" \
  "platforms;android-37.0" \
  "build-tools;37.0.0" \
  "system-images;android-37.0;google_apis;x86_64" \
  "system-images;android-26;google_apis;x86_64"
```

`sdkmanager` prints a deprecation notice pointing to the newer `android sdk` command. It is harmless. The tool is still the documented way to install these packages here.

### 6. KVM access

**Owner step:** give the user permanent access to `/dev/kvm`:

```bash
sudo usermod -aG kvm "$USER"
```

Then log out and log in again. Without the group, access may depend on a login-session ACL that does not reach every process and disappears on logout.

### 7. Virtual devices

Two AVDs on the `pixel_6` profile, one per system image, each with 2 GB of RAM. `cn-api37` is the default for development and end-to-end tests; `cn-api26` checks the minimum supported version.

```bash
avdmanager create avd --name cn-api37 \
  --package "system-images;android-37.0;google_apis;x86_64" --device pixel_6 </dev/null
avdmanager create avd --name cn-api26 \
  --package "system-images;android-26;google_apis;x86_64" --device pixel_6 </dev/null

for avd in cn-api37 cn-api26; do
  sed -i 's/^hw.ramSize=.*/hw.ramSize=2048/' "$HOME/.android/avd/$avd.avd/config.ini"
done
```

`</dev/null` answers the "custom hardware profile" question with its default (no). Messages such as `Could not load devices from .../devices.xml` are harmless: those files do not exist in the images and the built-in profiles are used.

## Verify

Run everything in a **new login shell** (`bash -lc '...'`), which is what agent panes get. Each block maps to a scenario of the `development-environment` spec.

### Java version check in a fresh shell

```bash
bash -lc 'java -version; javac -version'
```

Both report 17 (`17.0.20.1` when recorded).

### SDK tools reachable

```bash
bash -lc 'echo "$ANDROID_HOME"; sdkmanager --list_installed; adb version; emulator -version'
```

All commands succeed. The installed list contains `platform-tools`, `emulator`, `build-tools;37.0.0`, `platforms;android-37.0` and both system images. The command-line tools do not appear in the list because they were unpacked by hand.

### Licenses accepted

```bash
bash -lc 'set -o pipefail; sdkmanager --licenses </dev/null 2>&1 | tr "\r" "\n" | grep -Fx "All SDK package licenses accepted."'
```

Prints `All SDK package licenses accepted.` and exits 0 when every license is accepted. Exits non-zero otherwise.

`sdkmanager` alone is not a check: it exits 0 even when licenses are pending, and then prints `N of M SDK package licenses not accepted.` instead. The `grep` on the exact success line is what makes the command fail. `tr` splits the progress bar, which uses carriage returns, from that line, and `pipefail` also fails the command if `sdkmanager` itself fails. Closed stdin (`</dev/null`) only stops `sdkmanager` from waiting for an answer to its review prompt; it never accepts anything.

To see the check fail without touching the real SDK, point it at an empty SDK root (all licenses count as pending there):

```bash
bash -lc 'set -o pipefail; d=$(mktemp -d); sdkmanager --sdk_root="$d" --licenses </dev/null 2>&1 | tr "\r" "\n" | grep -Fx "All SDK package licenses accepted."; echo "exit=$?"; rm -rf "$d"'
```

### Acceleration check

```bash
bash -lc 'emulator -accel-check'
```

Reports `KVM (version 12) is installed and usable.`

### Headless boot

Boot one AVD at a time; the machine is shared with other agents. Repeat for `cn-api26`.

```bash
bash -lc '
start=$(date +%s)
nohup emulator -avd cn-api37 -no-window -no-audio -no-boot-anim -no-snapshot-save \
  -gpu swiftshader_indirect >"${TMPDIR:-/tmp}/emulator-cn-api37.log" 2>&1 &
pid=$!
timeout 180 adb wait-for-device || { echo "boot timed out"; kill "$pid" 2>/dev/null; exit 1; }
until [ "$(adb shell getprop sys.boot_completed | tr -d "\r")" = "1" ]; do
  [ $(( $(date +%s) - start )) -gt 180 ] && { echo "boot timed out"; kill "$pid" 2>/dev/null; exit 1; }
  sleep 2
done
echo "boot took $(( $(date +%s) - start ))s"
adb emu kill
'
```

`sys.boot_completed` must reach `1` within 180 seconds. Measured at setup time, cold boot with KVM: `cn-api37` 26 s, `cn-api26` 13 s.

`-gpu swiftshader_indirect` is used because the AVDs are created with the host GPU disabled and no display is present.

After `adb emu kill`, confirm nothing is left running:

```bash
ps -eo pid,comm | grep -Ei 'qemu|emulator' || echo "no emulator processes"
```

## Uninstall

For the final environment cleanup. Removes everything this setup installed. Run the steps in this order.

1. Stop the emulators and the adb server, then delete the AVDs (needs the SDK still in place):

   ```bash
   adb emu kill 2>/dev/null
   avdmanager delete avd --name cn-api37
   avdmanager delete avd --name cn-api26
   adb kill-server
   ```

2. Remove the SDK:

   ```bash
   rm -rf "$HOME/Android/Sdk"
   rmdir "$HOME/Android" 2>/dev/null || true
   ```

   `rmdir` only removes the parent directory if it is empty.

3. Remove the environment block from `~/.profile`:

   ```bash
   sed -i '/# >>> custom-notifier android sdk >>>/,/# <<< custom-notifier android sdk <<</d' ~/.profile
   ```

   Open a new login shell and check that `echo "$ANDROID_HOME"` prints nothing. The blank line that preceded the block may remain; it is harmless.

4. Remove the leftover tool state. `~/.android` holds the adb keys, download caches and emulator settings created by these tools. If no other Android tooling uses this machine, remove all of it:

   ```bash
   rm -rf "$HOME/.android" "$HOME/.emulator_console_auth_token"
   ```

   Otherwise remove only what the AVD deletion left behind (`~/.android/cache`) and keep `adbkey` and `adbkey.pub`.

5. **Owner step:** remove the JDK:

   ```bash
   sudo apt remove --purge openjdk-17-jdk-headless openjdk-17-jre-headless ca-certificates-java java-common
   ```

   These are the JDK and the dependencies its installation pulled in. The command names them explicitly instead of using `apt autoremove`, which would also remove unrelated orphaned packages.

6. **Owner step:** remove the user from the `kvm` group, then log out and log in again:

   ```bash
   sudo gpasswd -d "$USER" kvm
   ```

Gradle caches (`~/.gradle`) appear later, once the Gradle project exists, and are not part of this setup. The cleanup phase removes them separately.
