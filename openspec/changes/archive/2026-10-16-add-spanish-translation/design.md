## Context

See proposal.md - Why. All texts are in `app/src/main/res/values/strings.xml` (about 90 entries, no literals in Kotlin); dates use `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` with the configuration locale (`alerts/DateText.kt`, product list, form). The notification channel is created in `NotificationAlertNotifier.createChannel()` at app start. Calendar events are written by `CalendarEventSync` (`:domain`), which compares the wanted `AlertEvent` (name, depletion date, alert date) with the stored record content, and `ProviderCalendarGateway`, which builds the texts. The system-change receiver handles boot, update, time and time zone. Owner decisions (2026-10-16): `values-es`, informal "tú", texts approved in the spec table, system language plus `localeConfig`, no in-app picker, channel and events follow the language (item 1 of #78), tests as in the proposal.

## Goals / Non-Goals

**Goals:**
- Adding a language later means adding one resource file and one `localeConfig` entry.
- A missing or mismatched translation fails the build or the tests, not the user.

**Non-Goals:**
- In-app language picker, regional variants, store listing.

## Decisions

### 1. Resources and `localeConfig`
`app/src/main/res/values-es/strings.xml` with every translatable key, texts exactly as in the spec table (for the Spanish quote character use `\'` only where needed). `app_name` and the format-only strings (`product_list_stock`, `product_list_stock_approximate_prefix`, `product_form_preview`, `stock_labeled`, `stock_restock_added`) get `translatable="false"` and are not repeated in `values-es`. `res/xml/locales_config.xml` lists `en` and `es`; the manifest's `application` gets `android:localeConfig`. The default locale stays English (`values`).
- Android Lint's `MissingTranslation` and `ExtraTranslation` already fail the build on missing keys (lint runs with zero warnings); the JVM parity test adds the placeholder check that lint does not do.
- Alternative: `generateLocaleConfig` in AGP. Accepted if it works with the current AGP without extra config (it reads `resources.properties`); otherwise the hand-written XML. The executor picks the one with less moving parts and records it in the PR.

### 2. Channel name
`createChannel()` already runs at app start; `createNotificationChannel` with the same id updates the name. It is also called from the system-change receiver on `ACTION_LOCALE_CHANGED`, added to the existing intent filter (the broadcast is exempt from implicit-broadcast limits). A per-app language change on Android 13+ restarts the app's activities and the next app start updates the name.

### 3. Language of calendar events
- `:domain`: `AlertEvent` gets `language: LanguageTag` (value class over a BCP 47 string). `CalendarEventSync.sync` takes the current `LanguageTag` as it takes the zone, from a `() -> LanguageTag` in `AppContainer` (the app context's configuration locale, read on every use). Since the record stores the content, a different language makes the content differ and the existing update path rewrites the event; the gone-record rule is unchanged, so a deleted event is not recreated.
- `:app`: the adapter builds the texts from resources of the given language (`createConfigurationContext` with that locale), so the texts and the stored tag always match.
- Room: column `language TEXT NOT NULL DEFAULT ''` on `calendar_event`, database version 5 with `MIGRATION_4_5`, `app/schemas/5.json` committed. Existing records get `''`, which differs from any real tag, so the first sync after the update rewrites them once in the current language.
- `ACTION_LOCALE_CHANGED` in the receiver runs the reconcile and the sync like the other system changes.

### 4. Testing
- JVM: a parity test that parses `values/strings.xml` and `values-es/strings.xml` and checks the same translatable keys and the same set of placeholders per key; `:domain` tests that a language change rewrites events and does not recreate a gone one; Room migration test from version 4; a Robolectric test with `@Config(qualifiers = "es")` for the medium date format (`11 mar 2026`) and the notification texts.
- Screenshots (Roborazzi, `es` qualifier): every screen and dialog that has existing English references, in light theme; the product form, the Settings screen and the banners also at the largest font size. Review each PNG for clipped or overflowing text: Spanish is about 20 to 30 percent longer. English references stay unchanged.
- E2E: one instrumented test class run with the app locale set to Spanish (per-app locale on API 33+ through `LocaleManager`, or the test's configuration), creating a product and checking Spanish texts on the list and form; plus a by-hand check on `cn-api37` that switching the device language renames the channel and rewrites a calendar event.

## Risks / Trade-offs

- [Longer Spanish texts clip or wrap badly] → Screenshot review at the largest font size; shorten a translation only with the owner's approval (texts are owner-approved).
- ["Solucionar" does not fit the calendar banner button] → Owner decision (2026-10-16): the banner already moves the button below the text when needed; if "Solucionar" still clips or breaks the layout at any tested size, the Spanish button keeps the English "Fix" (a future change may replace it with an icon). Record which one was used in the PR body.
- [Translations wrong for some Spanish-speaking regions (for example "Ajustes" vs "Configuración")] → Accepted: one neutral-leaning `values-es`, as decided; regional files can be added later.
- [Per-app language change on Android 13+ does not send `LOCALE_CHANGED`] → Events and the channel name catch up at the next app start or product change; accepted.

## Migration Plan

Room migration 4 to 5 adds a column with a default; the first sync after the update rewrites existing events once. Rollback is reverting the PR before any release.
