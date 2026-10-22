# release-distribution Specification

## Purpose
Defines how the app is versioned and released: the version generated from Conventional Commits, the signed release bundle built only for a release, and the store listing, privacy policy and owner steps needed to distribute it through the Google Play internal testing track.

## Requirements

### Requirement: Version generated from Conventional Commits
The app version (version name) SHALL be generated from the Conventional Commits merged on `main` and kept in exactly one generated file in the repository; no agent and no person SHALL edit that file or `CHANGELOG.md` by hand. The version code SHALL be derived from the version name so that a later version always has a strictly greater version code.

#### Scenario: New releasable commits
- **GIVEN** a `feat` or `fix` commit merged on `main` since the last release
- **WHEN** the release automation runs on `main`
- **THEN** one release pull request is open (created or updated) that sets the next version and adds the matching `CHANGELOG.md` entry, and no other file changes

#### Scenario: Version code grows
- **GIVEN** two version names where the second is a later version than the first (for example 0.2.0 and 0.2.1, or 0.9.3 and 0.10.0)
- **WHEN** the app is built with each of them
- **THEN** the second build has a strictly greater version code than the first

### Requirement: Release on merge of the release pull request
Merging the release pull request SHALL create the version tag and the GitHub release, and SHALL build the release Android App Bundle signed with the upload key. The signed bundle and the R8 mapping file SHALL be kept as a downloadable artifact of that run, and the run SHALL fail if the bundle is not signed with the upload key.

#### Scenario: Release pull request merged
- **GIVEN** the owner has stored the upload key secrets
- **WHEN** the owner merges the release pull request
- **THEN** the tag and the GitHub release for that version exist, and the run provides an artifact with a bundle whose signature verifies and whose version name and code match the release, plus its mapping file

#### Scenario: Signing secrets missing
- **GIVEN** the upload key secrets are not stored
- **WHEN** a release is created
- **THEN** the bundle job fails with a message naming the missing secret, and no unsigned bundle is offered as the release artifact

### Requirement: Keys only in the release job
The upload key and its passwords SHALL exist only as GitHub Actions secrets and SHALL be available only to the job that builds the release bundle on `main`. They SHALL never be committed, printed in a log, or available to a pull request run.

#### Scenario: Pull request run
- **GIVEN** any pull request, including the release pull request
- **WHEN** its checks run
- **THEN** no signing secret is referenced by any job of that run

#### Scenario: Repository audit
- **GIVEN** the repository content and history of this change
- **WHEN** it is searched for keystores and key passwords
- **THEN** none is found, and the build configuration reads the key only from environment variables

### Requirement: Release pull request passes the required checks
The release pull request SHALL trigger the same required checks (`verify` and `e2e`) as any pull request, so that it can be merged under the `main` ruleset without bypassing it.

#### Scenario: Release pull request opened
- **GIVEN** the release token is stored
- **WHEN** the release automation opens or updates the release pull request
- **THEN** `verify` and `e2e` run on it

### Requirement: Store listing kept in the repository
The store listing SHALL be kept in the repository in English and Spanish: title (at most 30 characters), short description (at most 80 characters), full description (at most 4000 characters), and phone screenshots per language, plus the 512 x 512 icon and the 1024 x 500 feature graphic. The texts SHALL describe only what the app really does and SHALL be approved by the owner.

#### Scenario: Listing check
- **GIVEN** the store listing files
- **WHEN** they are inspected
- **THEN** both languages have a title, short and full description within the limits, at least two phone screenshots each showing the app in that language, and the icon and feature graphic have the exact sizes

### Requirement: Privacy policy
A privacy policy in English and Spanish SHALL be published in the repository and reachable at a stable public URL used in the store listing. It SHALL state that all data stays on the device, that there is no account, no analytics, no ads and no data sharing, that calendar access is used only to write the user's own alert events in the calendar the user chooses, that notifications are local, and how to contact the developer.

#### Scenario: Policy matches the app
- **GIVEN** the published privacy policy
- **WHEN** it is compared with the app's declared permissions
- **THEN** every permission that touches user data (calendar read and write, notifications) is explained, and no data collection is claimed that the app does not do or omitted that it does

### Requirement: Documented owner steps
The repository SHALL document every owner-only step needed for the first release and for each later release: Play Console app creation, Play App Signing, upload key generation and backup, the secrets and the release token, Data safety, content rating, target audience, ads declaration, app access, testers, uploading the bundle (which already contains the mapping file) and pasting the release notes from the repository on the internal testing track, and installing the build from Play. Agents SHALL NOT perform these steps.

#### Scenario: Owner follows the guide
- **GIVEN** a verified Play developer account and no prior setup
- **WHEN** the owner follows the release guide from the start
- **THEN** the owner can publish a release to the internal testing track and install it from Play on a physical device without information from outside the guide

### Requirement: Play build checked on a physical device
The first build distributed through the internal testing track SHALL be checked by hand on a physical device before the distribution issue is closed.

#### Scenario: First internal release
- **GIVEN** the first release installed from the Play internal testing track on a physical Android device
- **WHEN** the user creates a product, edits it, restocks it, receives an alert notification, and turns the calendar channel on so that an alert event is written
- **THEN** every step works as on the emulator, with no crash
