# SESSION0 - custom-notifier
_Date: 2026-09-22_

## Problem
Households with high and variable consumption of recurring supplies (medication, vitamins, diapers, household consumables) struggle to restock in time. Consumption depends on circumstances that change (more or less usage some days, unexpected family situations), and the math of "when will this run out" is easy to get wrong when many products are tracked at once.

Current workarounds do not fit:
- Dozens of one-off phone alarms, manually maintained.
- Recurring purchase subscriptions (e.g. online retailer subscriptions) are rigid: deliveries arrive while stock remains, or too late when consumption spikes. They also do not cover specific items such as medication.
- No existing alerting tool models "projected depletion of a consumable" with a user-defined lead time.

## Target user
A single person managing supplies for a household, often with several dependents. They know their own consumption patterns roughly (e.g. "about 3 pills every 2 days") but do not want to do the depletion math themselves. They are not assumed to be technical.

The MVP is single-user: no shared households, no accounts, no server.

## Vision
The goal is that a product never runs out at home. The user gets an alert with enough lead time to plan and make the purchase on their own. Alerts fit into the user's normal life flow: a push notification and/or an event in the calendar they already use.

When the user opens the app, they see the estimated stock of each product, compare it with what they actually have at home, correct it in a couple of taps if needed, and trust that the next alert date is recalculated automatically.

## Scope

### In
- Products that deplete over time (consumables).
- Stock tracked in units (pills, diapers, ml...), restocked by packages.
- Package size per product: fixed, but editable by the user at any time.
- Consumption rate entered manually by the user (e.g. 3 units every 2 days).
- Estimated stock computed automatically over time from the last known stock and the consumption rate.
- Adjustments: user adds or subtracts N units manually; depletion date and alert are recalculated automatically.
- Restock: adds one package size worth of units; can be tapped repeatedly (+1, +2, +3 packages).
- Depletion date calculation.
- One rule per product: alert N days (lead time) before the depletion date.
- Exactly one alert per rule.
- Next alert preview in the app.
- Alert channels: local push notification and calendar event (opt-in, with explicit calendar permission).
- Calendar events are created, moved and deleted in sync with changes in the app.
- Displayed stock is an integer rounded down, with a discreet UI note indicating rounding. The exact value is kept internally.
- Stock at zero is shown as "0", more prominent than normal but without error styling.
- Local data storage only (SQLite), within the app's private storage.
- Spanish translation as the final part of the MVP (development language is English).
- Resource footprint measurement (APK size, RAM, background battery usage), aiming for the minimum possible.

### Out
- iOS in the MVP (planned as a later phase, in a separate repository).
- Shared households, multiple users, accounts, sync, any backend or server.
- Per-person or age-based dosing schedules.
- Automatic calculation or learning of the consumption rate.
- Multiple alerts or reminders per rule.
- Multiple rules per product.
- Duplicate product detection (the user may create two similar products with different rules; that is their choice).
- Dose/intake reminders (e.g. "take the pill at 8:00").
- Expiration dates.
- Purchasing from the app.
- Data backup or export in the MVP (data loss is acceptable for the MVP).

### Pending
- Security review before public release, including encryption at rest.
- Data backup/export before public release.
- Pricing model for the public release.
- Whether additional alerts, reminders or other backlog items enter, based on tester feedback.

## Glossary
| Term | Agreed definition |
|------|-------------------|
| Product | A consumable that must be restocked (vitamins, diapers, detergent...). |
| Unit | What is actually counted and consumed (pill, diaper, ml). Stock is always tracked in units. |
| Package size | Number of units in one package of a product (e.g. 90 pills per bottle). Fixed per product, editable at any time. |
| Stock | Units of a product remaining. |
| Estimated stock | Units the app computes should remain today, based on the last adjustment and the consumption rate. Shown to the user so they can compare it with reality. |
| Consumption rate | Units consumed per time period, entered manually by the user (e.g. 3 units every 2 days). |
| Depletion date | Date on which a product is projected to run out, given its estimated stock and consumption rate. |
| Lead time | Number of days before the depletion date when the alert fires (e.g. 10). |
| Rule | The configuration that decides when to alert for a product. One rule per product in the MVP. |
| Alert | The notice delivered to the user when a rule is met, via one or more channels. |
| Channel | Delivery method for an alert: local push notification and/or calendar event. The calendar channel is opt-in and requires permission. |
| Adjustment | Manual correction of stock by the user (+/- N units). Triggers recalculation of the depletion date and the alert. |
| Restock | Adding one package size worth of units to stock. Can be applied repeatedly (+1, +2, +3 packages). |
| Next alert | In-app preview of the date on which a product's next alert will fire. |
| Goal | A product never runs out at home; the user has enough lead time to plan the purchase themselves. |

## Phases
### Phase 1 - MVP (Android)
Native Android app including:
- Core: product CRUD, units, package size, consumption rate, estimated stock, adjustments, restock, depletion date calculation.
- Next alert preview.
- Alert channels: local push notification and synchronized calendar event.
- Local storage in SQLite.
- Resource footprint measurement.
- Spanish translation as the closing step.
- Distribution to a real tester through the Google Play Console internal testing track.

Success criterion: for 8 weeks, the tester manages their recurring products using only the app, none of them runs out, and they remove the equivalent standalone phone alarms.

### Phase 2 - Public release hardening
- Security review and encryption at rest.
- Data backup/export.
- Registration of rights over the app (intellectual property and name/trademark). This is a mandatory step before publishing to any store and is always performed by the owner, never by an agent.
- Google Play closed testing requirement for new personal developer accounts (at least 12 testers for 14 days) before production access.
- Paid release on Google Play at a reasonable price.

### Phase 3 - iOS
Native iOS app (Swift/SwiftUI) in a separate repository, with iCloud Calendar integration.

### Final phase - Environment cleanup
Once the project reaches a stable released version (not just the MVP), uninstall the local development toolchain (JDK, Android SDK, emulator images) that was installed only to build this project.

### Backlog (unprioritized)
- Multiple alerts or reminders per rule.
- Shared households.
- Per-person or age-based dosing schedules.
- Automatic consumption rate.
- Duplicate product detection.
- Intake reminders and expiration dates.

## Constraints and technical decisions
- **Native apps per platform, no cross-platform framework.** Android in Kotlin with Jetpack Compose; iOS later in Swift/SwiftUI. Rationale: each OS has its own release cycle, requirements and vulnerabilities; one platform changing must not force a release of the other. Platform-specific code has given the best results in the owner's experience. The cost of double maintenance is to be mitigated with solid CI/CD.
- **Two repositories**, one per platform. This repository is the Android app.
- **Minimum Android version:** 8.0 (API 26).
- **No backend.** All data stays on the device. No accounts, no server costs.
- **Storage:** SQLite (via an Android persistence library such as Room) in the app's private storage, with parameterized queries. Stored content is never interpreted or executed, preventing injection through stored data.
- **Alert scheduling:** the depletion date is deterministic, so alerts are scheduled once with the OS and rescheduled only when the user changes something. No background polling, to keep battery usage near zero.
- **Calendar integration:** Android Calendar Provider (on-device calendar, synced by the OS with the user's Google account). No OAuth, no Google Cloud project, no app verification. The user chooses which calendar receives events.
- **Language:** all code, specs and terms in English. Spanish is added as a translation at the end of the MVP.
- **No personal references** in project artifacts; always refer to "the user".
- **Resource footprint:** minimal APK size, RAM and battery usage; to be measured, not assumed.
- **No deadlines.** This is the owner's main project, worked on continuously.
- **Distribution for testing:** Google Play Console internal testing track (one-time developer account fee).

## Open questions
- Pricing model for the public release.
- Which rights registration path to use and its cost.
- Scope and timing of the security review and encryption at rest.
- Backup/export format and mechanism for the public release.
- Which backlog items to prioritize after the 8-week validation, based on tester feedback.
- UX details of the calendar event (title, time of day, reminders inside the event) to be defined during specification.
