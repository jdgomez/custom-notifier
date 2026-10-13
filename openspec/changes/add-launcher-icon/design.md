## Context

`AndroidManifest.xml` points `android:icon` at `@android:drawable/sym_def_app_icon`, a placeholder from `add-lint-setup`; there is no `roundIcon`. `minSdk` is 26, so every supported device understands adaptive icons. The design was settled with the owner in two exploration rounds outside the repository (vector traces and previews in the executor's scratch directory, never committed). The owner chose variant "C1 dark" and asked for no further tuning, because the design may change later.

## Goals / Non-Goals

**Goals:**
- Ship the chosen mark as an adaptive icon with a monochrome layer, from vector resources only.
- Guard the rendered icon with pixel-exact screenshot references and check on a device that the app no longer uses the system icon.

**Non-Goals:**
- Redesigning or refining the mark, store graphics, theme colors, splash screen.

## Decisions

### Owner decisions (2026-10-13)
- Variant C1: the mark simplified for small sizes (no thin outer crescent strokes, thicker ring that still thickens toward the lower left, star with concave arms and a dense center), scaled to fill the 66dp safe zone.
- Colors: white `#FFFFFF` mark on pure black `#000000` (no Material 3 purple).
- Keep the reference's tilt of about 45 degrees.
- No further tuning: the slightly asymmetric center of the reference is not reproduced.

### Vector resources only, in `mipmap-anydpi`
`res/mipmap-anydpi/ic_launcher.xml` (adaptive icon with background, foreground and monochrome) and `ic_launcher_round.xml` with the same content; the manifest sets `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`. With `minSdk` 26 no `-v26` qualifier and no PNG fallbacks are needed. Alternative rejected: Image Asset Studio PNG mipmaps per density, which add binary files that are harder to review and change, with no benefit on API 26+.

The foreground is a 108dp VectorDrawable (`res/drawable/ic_launcher_foreground.xml`) taken from the chosen exploration output (`variant-C1d`), cleaned up to match the repo's XML style. The background is a color resource `ic_launcher_background` (`#000000`) in `res/values/ic_launcher_colors.xml` (or the existing colors file, if one exists).

### One drawable for foreground and monochrome
The system uses only the alpha of the monochrome layer, and the foreground is already a single-color mark on a transparent canvas, so the monochrome layer references the same `ic_launcher_foreground` drawable. Alternative rejected: a separate monochrome file with identical paths, which duplicates the geometry and can drift.

### Verification
- JVM (Robolectric + Roborazzi, `./gradlew test`): render the adaptive icon resource at a fixed size with a circle mask, and the monochrome layer alone, and verify them pixel-exact against committed references in `app/src/test/screenshots/`. This catches accidental edits to the paths.
- Instrumented (`scripts/e2e.sh`): the installed app's application icon resolves to an `AdaptiveIconDrawable` that is not the system default icon, and on API 33+ it has a monochrome layer.
- Manual, recorded as PR evidence: launcher screenshots on `cn-api26` and `cn-api37` (app list, and on API 37 with themed icons on). Lint's `MonochromeLauncherIcon` check also covers the monochrome layer.

## Risks / Trade-offs

- Pure black loses the icon's edge on very dark wallpapers → accepted by the owner when choosing the dark variant.
- Robolectric's render is not the launcher's → the screenshot references guard against regressions only; the visual check on real emulators is the acceptance evidence.
- The design may change later → a future change replaces one VectorDrawable and re-records the references; nothing else depends on the geometry.
