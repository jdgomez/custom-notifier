## 1. Top bar

- [ ] 1.1 Make the product form top bar grow with its title (Material 3 medium top bar, design choice approved by the owner as option A), for both the new and the edit form; keep the default font size look
- [ ] 1.2 Add Robolectric/Roborazzi coverage for the Spanish edit form at the largest font size (title fully shown, save action visible) and re-record only the references that change; review the PNGs

## 2. Inline rows

- [ ] 2.1 Group each number field with its following word in the inline rows (rule row and alert row) so they wrap by pairs
- [ ] 2.2 Add coverage for the rule row at the largest font size in English and Spanish, and keep the default-size references unchanged

## 3. Check

- [ ] 3.1 Hand check on `cn-api37` at font scale 1.0 and 2.0, English and Spanish, new and edit form; restore font scale 1.0 and shut the emulator down afterwards (only when Brain says the emulator is free)
- [ ] 3.2 `./gradlew assembleDebug assembleRelease lintAll test` passes with zero warnings

## 4. Archive

- [ ] 4.1 Run `openspec archive fix-layout-overlaps`, sync the delta spec into `openspec/specs/product-form/`, and update the Project state in `AGENTS.md`
