# development-environment Specification

## Purpose
Defines what the development machine must provide so agents and the owner can build, lint and test the Android app from the command line, and how that setup is verified, reproduced and removed.

## Requirements

### Requirement: Java toolchain available
The development machine SHALL provide JDK 21 as the default `java` and `javac` on the `PATH` of any new shell, including shells started inside Herdr panes, and SHALL have no other JDK installed once the migration from JDK 17 is complete.

#### Scenario: Java version check in a fresh shell
- **GIVEN** a new login shell on the development machine
- **WHEN** the user runs `java -version` and `javac -version`
- **THEN** both report major version 21

#### Scenario: Single JDK on the machine
- **GIVEN** the owner has completed the documented JDK 17 removal step
- **WHEN** the user lists the installed Java alternatives
- **THEN** only the JDK 21 installation is listed

### Requirement: Android SDK available
The development machine SHALL provide an Android SDK in the user's home directory, exposed through the `ANDROID_HOME` environment variable, with `sdkmanager`, `adb` and `emulator` reachable on the `PATH`.

#### Scenario: SDK tools reachable
- **GIVEN** a new login shell on the development machine
- **WHEN** the user runs `sdkmanager --list_installed`, `adb version` and `emulator -version`
- **THEN** each command succeeds, and the installed list contains platform-tools, build-tools, one Android platform, the emulator, and x86_64 system images for the latest stable API level and API 26

#### Scenario: Licenses accepted
- **GIVEN** the SDK is installed
- **WHEN** a build tool asks the SDK for components
- **THEN** it does not stop on an unaccepted license prompt

### Requirement: Hardware-accelerated emulator
The development machine SHALL provide Android Virtual Devices at the latest stable API level and at the minimum supported API level (26), each booting with KVM hardware acceleration and able to run headless.

#### Scenario: Acceleration check
- **GIVEN** the emulator is installed
- **WHEN** the user runs `emulator -accel-check`
- **THEN** it reports that KVM is installed and usable

#### Scenario: Headless boot
- **GIVEN** a documented AVD exists
- **WHEN** the user starts it headless (no window) and waits for boot
- **THEN** `adb shell getprop sys.boot_completed` returns `1` within 3 minutes

### Requirement: Setup is documented, reproducible and removable
The repository SHALL contain a development setup guide listing the exact installed versions, the commands to reproduce the setup on a clean machine, the commands to verify it, and the commands to uninstall every installed component.

#### Scenario: Reproduce from the guide
- **GIVEN** a reader with only the repository and the guide
- **WHEN** they follow the installation section
- **THEN** they end with the same components and versions, verified by the guide's verification section

#### Scenario: Remove from the guide
- **GIVEN** the project reaches its final environment cleanup phase
- **WHEN** the owner follows the uninstall section
- **THEN** the JDK, the SDK directory, the AVDs and the related environment variables are removed, and nothing installed by this change remains
