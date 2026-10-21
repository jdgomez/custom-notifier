# Releasing to the Google Play internal testing track

Owner-only guide ([ADR 0011](adr/0011-internal-testing-distribution.md), [0014](adr/0014-play-app-signing.md), [0018](adr/0018-release-with-release-please.md)). Agents never perform these steps and never see the upload key. How it works: release-please keeps a release pull request open on `main`; merging it creates the tag and the GitHub release, and the `release` workflow builds the signed bundle and keeps it as a workflow artifact; you upload that bundle to Play by hand.

## One-time setup

### 1. Create the app in the Play Console
1. Play Console, **Create app**: name `Custom Notifier`, default language English (United States), type App, free. Accept the declarations.
2. Use Play App Signing (the default for new apps): Google holds the app signing key and you hold the upload key. The package name `dev.jdgomez.customnotifier` becomes permanent with the first upload.

### 2. Generate the upload key
Do this on your own machine, not in an agent session. Generate and encode the key outside the repository, in a directory only you can read, so key material never lands in a checkout.

```
mkdir -m 700 ~/keys && cd ~/keys
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 9125
```

Answer the prompts: the keystore password (at least 6 characters; the key password can be the same) and the certificate details. Then check the key exists, copy the "SHA256" fingerprint and encode it, still in `~/keys`:

```
keytool -list -v -keystore upload.jks -alias upload
base64 -w0 upload.jks > upload.jks.base64
```

- **Back up** `upload.jks` and both passwords in your password manager plus one offline copy. Never commit them; `.gitignore` also excludes `*.jks`, `*.keystore` and `*.base64`, as a safety net only: do not create these files inside the repository.
- If the key is lost, Play can reset it: Play Console, Setup, App signing, **Request upload key reset**. Then generate a new key, replace the secrets and the fingerprint variable below, and upload with the new key.

### 3. GitHub secrets and variable
Repository, Settings, Secrets and variables, Actions:

| Name | Kind | Content |
| --- | --- | --- |
| `UPLOAD_KEYSTORE_BASE64` | secret | the content of `upload.jks.base64` |
| `UPLOAD_KEYSTORE_PASSWORD` | secret | the keystore password |
| `UPLOAD_KEY_ALIAS` | secret | `upload` |
| `UPLOAD_KEY_PASSWORD` | secret | the key password |
| `UPLOAD_CERT_SHA256` | variable | the SHA256 fingerprint from step 2 (public value; the release run fails if the bundle is signed with another key) |
| `RELEASE_PLEASE_TOKEN` | secret | the token of step 4 |

Delete `~/keys/upload.jks.base64` afterwards.

### 4. The release token
Pull requests opened with the default workflow token do not trigger CI, and the `main` ruleset requires `verify` and `e2e`, so the release pull request is opened with a token of yours.

1. GitHub, Settings, Developer settings, Fine-grained personal access tokens, **Generate new token**.
2. Resource owner: you; repository access: **only** `jdgomez/custom-notifier`; permissions: Contents **read and write**, Pull requests **read and write**; set an expiry (for example 1 year).
3. Store it as `RELEASE_PLEASE_TOKEN`. Put the expiry date in your calendar: when the token expires, the `release-please` job of the `release` workflow fails on every push to `main`. Renew by generating a new token and replacing the secret.
4. The token cannot push to `main` (the ruleset has no bypass). A GitHub App is a sturdier replacement if you ever want one.

### 5. Play Console declarations
Under Dashboard, **Set up your app**:

- **Privacy policy:** `https://github.com/jdgomez/custom-notifier/blob/main/docs/privacy-policy.md`.
- **App access:** all functionality is available without logging in.
- **Ads:** the app contains no ads.
- **Content rating:** answer the questionnaire truthfully (a utility app: no violence, no user-generated content, no sharing of location, no purchases).
- **Target audience:** adults (not directed at children).
- **Data safety:** no data collected and no data shared. The app reads and writes the device calendar only to put the user's own alert events there, and posts local notifications; nothing leaves the device (there is no internet permission), so under Play's definition nothing is "collected". Data encrypted in transit: not applicable. Data deletion: not applicable (data stays on the device and is deleted on uninstall).
- Other declarations (government app, financial features, health, news): no.
- If the Console asks about a permission, the answers are in [the privacy policy](privacy-policy.md): notifications are local, calendar access writes the user's own alert events in the calendar the user chooses.

### 6. Store listing
Main store listing, default language English (United States): copy the title, short description and full description from `fastlane/metadata/android/en-US/`, and upload `images/icon.png`, `images/featureGraphic.png` and the screenshots from `images/phoneScreenshots/`. Add the Spanish (Spain) translation from `es-ES/` with its screenshots. Add your contact email (Play shows it; the repository does not contain it).

### 7. Testers
Testing, Internal testing, Testers: create an email list and add the Google accounts of the testers (including yours). Copy the **opt-in link**; testers open it on their device and install from Play.

## Each release

1. **Merge the release pull request.** Open it (title `chore(main): release X.Y.Z`), check that it changes only `version.txt`, `CHANGELOG.md` and `.release-please-manifest.json`, wait for green `verify` and `e2e`, and merge it (squash). The release pull request does not go through the no-mistakes gate.
2. **Wait for the `release` run** on `main` (Actions). It creates the tag `vX.Y.Z` and the GitHub release, then the `bundle` job builds and checks the signed bundle. If it fails, the message names the missing secret, the signature, or the version that is wrong.
3. **Download the artifact** `release-X.Y.Z` from the run: `app-release.aab` and `mapping.txt` (kept 90 days).
4. **Upload to Play.** Testing, Internal testing, **Create new release**: upload `app-release.aab`; Play assigns no version of its own, the version name and code come from the bundle. Then, on the uploaded bundle, upload `mapping.txt` as the **deobfuscation file** so crash stack traces are readable. Release notes: paste the entry of this version from `CHANGELOG.md` (English, and Spanish if you want). Save, review, **Start rollout to Internal testing**.
5. **Install from Play** on the tester's device through the opt-in link and check it (below for the first release).

Store screenshots are refreshed in any change that visibly changes the main screens (a pull request reviewer checks this); upload the new ones in the listing.

## First release: check on a physical device

Before issue #47 is closed, install the first build from the internal testing track on a physical Android device and check, with no crash:

1. create a product;
2. edit it;
3. restock it;
4. receive an alert notification (for example a product whose alert is due soon);
5. turn the calendar channel on in Settings and see the alert event written in the chosen calendar.

Report any difference from the emulator as an issue.

## Automating the upload later

Uploading through the Play Developer API needs a service account and a new dependency; it is deliberately not part of this setup. After a few releases, decide whether it is worth it; `fastlane/metadata/android/` already has the standard layout.
