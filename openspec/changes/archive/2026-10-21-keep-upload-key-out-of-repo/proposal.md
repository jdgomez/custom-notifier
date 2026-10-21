## Why

Generating the Play upload key with the steps in `docs/release.md` left an encoded copy of the key (`upload.jks.base64`) inside the repository checkout, untracked and not git-ignored, so a careless `git add` could have committed it. Decided by the owner on 2026-10-21.

## What Changes

- `.gitignore` also excludes `*.base64` in the signing material section.
- `docs/release.md` generates, checks and encodes the upload key in a private directory outside the repository (`~/keys`), so the steps never write key material in the checkout.

No app behavior changes, so there is no spec delta. No new dependencies.

## Impact

- Touches: `.gitignore`, `docs/release.md`.
