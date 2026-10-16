# custom-notifier

An Android app that tells you when a household consumable is about to run out, early enough to buy more in time.

You track things you use up: vitamins, diapers, detergent. You tell the app how much you have, how fast you go through it, and how many days of warning you want. The app estimates what is left day by day and alerts you before it runs out, as a notification and, optionally, as an event in your calendar. Buying is up to you; the app just makes sure you are never caught short.

> Status: early stage. The user can list, create, edit and delete products and record restocks and stock adjustments; the pure-Kotlin product, stock, rule, depletion date and next alert model is in `:domain`. Alerts are scheduled and delivered as local notifications, the app asks for the notification permission after a product is saved and warns while alerts are off. A Settings screen turns the calendar channel on and chooses the device calendar; the product list warns while the calendar channel is on but cannot work; the app keeps one all-day event per product with a next alert in that calendar.

## How this project is built

This project is built with agent engineering: the owner does not write the code. Agents plan, implement and review each change, while the owner approves proposals, reviews UX and merges every pull request. The process itself is part of the project, and it is documented in the open.

- [`docs/SESSION0.md`](docs/SESSION0.md) - what is being built: problem, users, scope, glossary and phases.
- [`docs/WAYOFWORKING.md`](docs/WAYOFWORKING.md) - how it is built: roles, lifecycle of a change, definition of done, testing strategy and conventions.
- [`docs/development-setup.md`](docs/development-setup.md) - the local toolchain: JDK, Android SDK and emulators, with exact versions and how to install, verify and remove them.
- [`docs/adr/`](docs/adr/README.md) - architecture decision records: why the technical choices were made.
- `openspec/` - specs and changes; the source of truth for behavior.
- GitHub issues, milestones and the project board track every change from proposal to merge.

## Planned stack

- Native Android: Kotlin, Jetpack Compose, minimum API 26.
- Pure Kotlin domain module, with no Android dependencies.
- Local storage only (SQLite). No backend, no accounts, no cloud.
- Calendar integration through the Android Calendar Provider.
- An iOS app is planned later, natively, in a separate repository.
