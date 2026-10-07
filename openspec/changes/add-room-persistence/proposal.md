## Why

The domain can model products, their stock, depletion date and next alert, but nothing survives the app being closed. Every next Phase 1 change (product list, create/edit/delete, adjustments, notifications, calendar) needs products stored on the device and read back exactly as they were saved.

## What Changes

- A product gets an **identity**, assigned when it is created and never changed by edits, so it can be saved, found, updated and deleted, and later linked to its scheduled alert and calendar event.
- A **product repository** port in the domain: observe all products (updates are pushed when anything changes), find one by identity, save (create or replace), delete.
- A **Room adapter** in `:app` that implements the port with SQLite in the app's private storage (ADR 0006), storing every value losslessly, including the exact fractional stock and its recording moment.
- **Manual dependency injection**: one application-wide container builds the database and the repository. Recorded in a new **ADR 0015**.
- The database schema (version 1) is exported and committed; data is never dropped on a schema change.

## Capabilities

### New Capabilities
- `product-persistence`: products are stored on the device, survive the app restarting, and are read back with exactly the values they were saved with.

### Modified Capabilities
- `product-stock`: adds the product identity requirement.

## Non-goals

- Any UI (`#42`, `#43`, `#44`).
- Schema migrations: version 1 has none. The first schema change adds them, with a migration test.
- Backup and restore: backup is already disabled in the manifest (ADR 0005).
- Encrypting the database: data is non-sensitive and lives in private storage.
- Storing alert delivery state (`#45`) or calendar event links (`#46`).

## Impact

- **Depends on:** `#40` (`add-depletion-and-alert-domain`, merged) and its archive PR `#54`, whose synced specs this change builds on.
- **Touches:** `:domain` (identity, repository port), `:app` (Room adapter, application class, container), `gradle/libs.versions.toml`, root and module build files, `docs/adr/` (new ADR 0015 and index).
- **New dependencies** (approving this proposal approves them):
  - `androidx.room3:room3-runtime` and `androidx.room3:room3-compiler` 3.0.1 (KSP processor).
  - The Room Gradle plugin (`androidx.room3`, same version) for schema export.
  - The KSP Gradle plugin (`com.google.devtools.ksp`), at the version compatible with Kotlin 2.4.20.
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core` in `:domain` (pure Kotlin, no Android), and its test artifact `kotlinx-coroutines-test` for tests.
  - Room's Android SQLite driver from the platform framework, not the bundled native SQLite (ADR 0010).
