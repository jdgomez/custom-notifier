## Why

The app still shows the system's generic placeholder icon (`@android:drawable/sym_def_app_icon`), set by `add-lint-setup` only so that Android Lint passes (issue #17). Users cannot tell the app apart in the launcher, and a store release (#47) needs a real icon.

## What Changes

- Add an adaptive launcher icon (background, foreground and monochrome layers) drawn as vector resources, and point the manifest's `android:icon` and `android:roundIcon` at it, replacing the placeholder.
- The design is the one the owner chose on 2026-10-13 after two exploration rounds (variant "C1 dark"): a white mark on a pure black (`#000000`) background, derived from a logo with personal meaning to the owner. The mark is a tilted ellipse ring (about 45 degrees) enclosing a four-pointed star with concave arms and a dense center. It is simplified for small sizes: no thin outer strokes and thicker ring and arms, so it reads at 48dp.
- The monochrome layer uses the same mark, so the icon follows the user's themed icons setting on Android 13 and later.
- Screenshot references of the rendered icon guard it against accidental changes.

No new dependencies.

## Non-goals

- Fine-tuning the mark further (owner decision: the design may change in the future; this change ships it as chosen).
- Brand colors for the app theme: the app theme is unchanged.
- Store listing graphics (feature graphic, high-resolution store icon): part of #47.
- Committing the original reference image: it stays out of the repository; the vector resources are the only source.
- A splash screen icon.

## Capabilities

### New Capabilities
- `launcher-icon`: the app's own adaptive launcher icon, its themed (monochrome) variant, and how it is verified.

### Modified Capabilities
- None.

## Impact

- Depends on: nothing (the placeholder from `add-lint-setup` is already on `main`).
- Touches: `:app` resources only (`res/mipmap-anydpi/`, `res/drawable/`, `res/values/`), `AndroidManifest.xml`, JVM screenshot tests and their references in `app/src/test/screenshots/`, and an instrumented check.
- Parallelizable with `add-alert-scheduler` (#45): no shared files except possibly `AndroidManifest.xml`, where the edits are on different attributes.
- Closes #17.
