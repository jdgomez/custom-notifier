# resource-footprint Specification

## Purpose
Defines the shrunk release build, how the app's resource footprint (size, RAM, storage, background battery usage) is measured reproducibly on an emulator, and where the recorded baseline lives.

## Requirements

### Requirement: Shrunk release build
The release build SHALL be shrunk and optimized (unused code and resources removed) and SHALL keep every user-visible behavior of the debug build. The release build configuration SHALL contain no signing key.

#### Scenario: Release build is smaller than debug
- **GIVEN** the current `main`
- **WHEN** the debug and the release APK are built
- **THEN** the release APK is smaller than the debug APK, and the release build configuration references no keystore

#### Scenario: Shrunk build behaves like debug
- **GIVEN** the release APK, signed locally with the debug key, installed on `cn-api26` and on `cn-api37`
- **WHEN** the user creates, edits, restocks, adjusts and deletes a product, receives an alert notification, turns the calendar channel on, and switches the device language to Spanish
- **THEN** every step behaves as in the debug build, with no crash

### Requirement: Reproducible footprint measurement
The project SHALL provide one documented command that measures the footprint of the release APK on the one running `google_apis` emulator, with a fixed set of seeded products, and reports: APK file size, estimated download size, RAM (PSS) with the product list open, with the product form open and in the background process woken without opening the app UI, installed app size, app data size, and the app's alarms, wakeups, wakelocks and jobs over an idle period with the screen off.

#### Scenario: Successful measurement
- **GIVEN** exactly one booted `google_apis` emulator and no other device
- **WHEN** the user runs the footprint command
- **THEN** it exits with success and writes, under `build/footprint/`, a machine-readable result and a human-readable summary containing every listed value, the API level, the commit and the date

#### Scenario: Wrong device setup
- **GIVEN** no emulator, more than one device, or an emulator without root access through adb
- **WHEN** the user runs the footprint command
- **THEN** it exits with failure before building anything and says which device setup it needs

#### Scenario: A value cannot be measured
- **GIVEN** a booted emulator on which one of the values cannot be obtained
- **WHEN** the footprint command runs
- **THEN** it exits with failure, names the missing value, and keeps whatever it measured under `build/footprint/`

### Requirement: Background work stays near zero
While the app is not in use, its only scheduled background work SHALL be one inexact alarm per product with a next alert. It SHALL hold no wakelock and schedule no job or periodic work.

#### Scenario: Idle device with seeded products
- **GIVEN** the seeded products installed and the app sent to the background with the screen off
- **WHEN** the footprint command observes the idle period
- **THEN** the app has at most one pending alarm per product with a next alert, no wakelock time, no jobs, and no wakeup other than those alarms

### Requirement: Recorded baseline
The repository SHALL contain a footprint document with the measurement method, how to run it, and the baseline results on the oldest supported API (26) and the latest API (37), each with the commit and date it was measured on. It SHALL state that no numeric budgets are set yet.

#### Scenario: Owner reads the baseline
- **GIVEN** the merged change
- **WHEN** the owner opens the footprint document
- **THEN** it shows every measured value for API 26 and API 37 with commit and date, and explains how to reproduce them
