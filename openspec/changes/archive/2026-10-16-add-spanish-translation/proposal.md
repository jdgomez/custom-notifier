## Why

SESSION0 makes Spanish part of the MVP, and the tester uses the phone in Spanish. All user-visible texts are already Android string resources in English (`app/src/main/res/values/strings.xml`) and dates already follow the device locale, so this change (#48) adds the Spanish resources and makes the parts that were written once in one language follow the language too: the notification channel name and the calendar events. Decided by the owner on 2026-10-16.

## What Changes

- Spanish resources in `values-es` (any Spanish-speaking region), informal "tú", with the texts approved in `specs/localization/spec.md`. "Custom Notifier" and what the user types (product names, units) are not translated.
- The app follows the device language. It declares its supported languages (`localeConfig`: English, Spanish), so on Android 13 and later the user can choose the app's language in the system settings. There is no language picker in the app.
- Texts that are not language (`app_name`, pure format strings such as `%1$s %2$s`) are marked `translatable="false"`.
- The notification channel name follows the language: it is updated at app start and when the device language changes.
- Calendar events follow the language (folds in item 1 of #78): the record of each event stores the language its texts were written in, and the next sync rewrites events written in another language. A device language change triggers the alarm reconcile and the calendar sync. Room database version 5 with a migration from version 4.
- Tests: a JVM test that `values-es` has exactly the translatable keys of `values` with the same placeholders; Spanish screenshot references of every screen and dialog in light theme, and at the largest font size for the screens with the longest texts; one Spanish E2E run.

No new dependencies.

## Non-goals

- A language picker inside the app.
- Regional variants (`es-ES`, `es-MX`, ...) or any other language.
- The Play Store listing in Spanish (#47).
- Translating user data or the app name.
- Item 2 of #78 (orphan pending ids); #78 stays open for it.

## Capabilities

### New Capabilities
- `localization`: supported languages, how the language is chosen, which texts are translated, the approved Spanish texts, and how the notification channel and calendar events follow a language change.

### Modified Capabilities
None. The English texts in the existing specs stay the source; `localization` gives their Spanish versions.

## Impact

- Depends on: #42 to #46 (all merged).
- Touches: `:app` resources (`values-es/strings.xml`, `xml/locales_config.xml`, `translatable` flags), manifest (`localeConfig`, `LOCALE_CHANGED` in the existing system-change receiver), the notification channel creation, the calendar event record and the Room migration 4 to 5 with `app/schemas/5.json`, `:domain` (`AlertEvent` comparison includes the language), tests and screenshot references. Not parallelizable with work that adds strings or touches the Room schema.
- Issue: Closes #48. Refs #78 (item 1 done here, the issue stays open for item 2).
