## 1. Icon resources

- [x] 1.1 Add `res/drawable/ic_launcher_foreground.xml` from the chosen exploration output (`~/.cache/agent-flow/w1F/icon-explore/round2/variant-C1d/`), white mark on a transparent 108dp canvas, cleaned up to the repo's XML style
- [x] 1.2 Add the `ic_launcher_background` color (`#000000`)
- [x] 1.3 Add `res/mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` (background, foreground, monochrome referencing the foreground)
- [x] 1.4 Point `android:icon` and `android:roundIcon` in `AndroidManifest.xml` at the new mipmaps, removing the placeholder; `./gradlew lintAll` clean

## 2. Tests

- [x] 2.1 Roborazzi JVM test rendering the adaptive icon with a circle mask and the monochrome layer alone; record the references and review the PNGs
- [x] 2.2 Instrumented test: the application icon is an `AdaptiveIconDrawable`, not the system default icon, and has a monochrome layer on API 33+
- [x] 2.3 `./gradlew assembleDebug lintAll test` and `scripts/e2e.sh` green

## 3. Visual verification

- [x] 3.1 Install on `cn-api26` and `cn-api37` (one at a time) and capture launcher screenshots: app list on both, and themed icons on with API 37; check the mark is whole and centered in the mask and readable at launcher size; attach as PR evidence
- [ ] 3.2 (owner) Approve the icon as it looks on the devices before merge
