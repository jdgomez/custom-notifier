## 1. Top bar

- [ ] 1.1 Product form top bar (owner decision 2026-10-21): keep the usual one-line top bar when the title fits next to the back arrow and the save action; switch to the Material 3 medium top bar only when the title does not fit, decided by measuring the title text against the available width (not by a font scale threshold); for both the new and the edit form; default font size references stay unchanged
- [ ] 1.2 Add Robolectric/Roborazzi coverage for the Spanish edit form at the largest font size (title fully shown, save action visible) and a test that the one-line bar is kept when the title fits and re-record only the references that change; review the PNGs

## 2. Inline rows

- [ ] 2.1 Group each number field with its following word in the inline rows (rule row and alert row) so they wrap by pairs
- [ ] 2.2 Add coverage for the rule row at the largest font size in English and Spanish, and keep the default-size references unchanged

## 3. Check

- [ ] 3.1 Hand check on `cn-api37` at font scale 1.0 and 2.0, English and Spanish, new and edit form; restore font scale 1.0 and shut the emulator down afterwards (only when Brain says the emulator is free)
- [ ] 3.2 `./gradlew assembleDebug assembleRelease lintAll test` passes with zero warnings

## 4. Archive

- [ ] 4.1 Run `openspec archive fix-layout-overlaps`, sync the delta spec into `openspec/specs/product-form/`, and update the Project state in `AGENTS.md`
