## MODIFIED Requirements

### Requirement: Shrunk release build
The release build SHALL be shrunk and optimized (unused code and resources removed) and SHALL keep every user-visible behavior of the debug build. The release build configuration SHALL contain no signing key and no key password; it MAY read the upload key from environment variables, and without them it SHALL produce an unsigned release build.

#### Scenario: Release build is smaller than debug
- **GIVEN** the current `main`
- **WHEN** the debug and the release APK are built
- **THEN** the release APK is smaller than the debug APK, and the release build configuration contains no keystore file, key or password

#### Scenario: Unsigned without the signing environment
- **GIVEN** no signing environment variables are set
- **WHEN** the release APK and bundle are built
- **THEN** the build succeeds and the output is unsigned, as before this change

#### Scenario: Shrunk build behaves like debug
- **GIVEN** the release APK, signed locally with the debug key, installed on `cn-api26` and on `cn-api37`
- **WHEN** the user creates, edits, restocks, adjusts and deletes a product, receives an alert notification, turns the calendar channel on, and switches the device language to Spanish
- **THEN** every step behaves as in the debug build, with no crash
