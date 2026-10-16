## 1. Resources

- [ ] 1.1 Mark `app_name` and the format-only strings `translatable="false"`; add `values-es/strings.xml` with every text exactly as in the spec table (design decision 1)
- [ ] 1.2 Declare the supported languages (`localeConfig`: `en`, `es`) (design decision 1)
- [ ] 1.3 JVM parity test: same translatable keys and placeholders in both files

## 2. Texts written outside the app

- [ ] 2.1 Update the channel name on `ACTION_LOCALE_CHANGED` and at start; add the action to the system-change receiver, which also runs the reconcile and the calendar sync (design decision 2)
- [ ] 2.2 Add `LanguageTag` to `AlertEvent` and the sync input; build the event texts from resources of that language; `:domain` tests for rewrite on a language change and no recreation of a gone event (design decision 3)
- [ ] 2.3 Add the `language` column, database version 5 with `MIGRATION_4_5`, commit `app/schemas/5.json`; migration test from version 4

## 3. Tests and checks

- [ ] 3.1 Robolectric tests in Spanish for the date format and the notification texts; Spanish screenshot references per design decision 4, each PNG reviewed for clipped or overflowing text (report any text that does not fit instead of changing an approved translation; the one pre-approved exception: if "Solucionar" does not fit the calendar banner button, use "Fix", see design Risks)
- [ ] 3.2 Spanish instrumented test per design decision 4; run `scripts/e2e.sh` on `cn-api37` and check the video; by hand on `cn-api37`, switch the device language to Spanish and check the channel name and a calendar event; boot `cn-api26` and check the app in Spanish; shut the emulators down
- [ ] 3.3 Check the production line count (about 400 at most, tests and resource files excluded); if clearly over, stop and report

## 4. Archive

- [ ] 4.1 Run `openspec archive add-spanish-translation`, sync the delta spec into `openspec/specs/localization/spec.md`, and update the Project state in `AGENTS.md` (#48 done; next #49)
