## Context

`:domain` holds `Product`, `Stock`, `EstimatedStock`, `Quantity` and the product value types (`add-product-stock-domain`, see its archived `design.md`). `Stock` is anchored: exact units (a reduced `BigInteger` fraction) plus the `Instant` they were recorded at; consumption is `units * elapsedMillis / (days * 86_400_000)`. Domain operations take `now: Instant` as a parameter; the app injects a `Clock`. Requirements are in `specs/depletion-and-alert/spec.md` and the modified `specs/product-stock/spec.md`; motivation in `proposal.md`.

## Goals / Non-Goals

**Goals:**
- Depletion date and next alert as pure, deterministic functions of the product, the time zone and (for the next alert) the current moment.
- One place that turns "local date and 09:00" into an `Instant`, so the notification and calendar changes reuse it instead of re-deriving it.

**Non-Goals:**
- Delivery state and the "exactly one alert" guarantee (`#45`).
- Any `Clock` or `ZoneId` lookup inside the domain: both are always inputs.

## Decisions

### Types follow the glossary
Same package as the existing types. Proposed types:
- `LeadTime`: whole days, `Int` in `0..365`, rejected otherwise with a message naming the lead time.
- `Rule`: holds a `LeadTime`. A dedicated type even with one field, because the glossary names it and later rules (backlog: more alerts per rule) would grow it, not `Product`.
- `Product` gains `rule: Rule`; `Product.create` takes it; `Product.changeRule(rule)` returns a copy with the stock untouched (like `rename`).
- `DepletionDate`: wraps the `LocalDate`, plus the exact depletion `Instant` it came from, if the executor finds callers need it (the notification change will); keep it a value type, not a bare `LocalDate`.
- `NextAlert`: sealed type with `Scheduled(at: ZonedDateTime)`, `Due` and `None`. `Scheduled` keeps the zoned time so the UI preview and the scheduler both get the local time and the `Instant` without recomputing.

Alternative considered: `Rule` folded into `Product` as a bare `leadTime`. Rejected: ADR 0012 and the glossary ask for a dedicated concept.

### Depletion moment from the anchor, not by searching
Depletion moment = `recordedAt + units * days * 86_400_000 / rateUnits` milliseconds, computed with `BigInteger` and floored to whole milliseconds, then converted to an `Instant`. The precision is an implementation detail, not a requirement: it matches the estimate so both always agree. This is the closed-form inverse of the existing estimate (same ms precision, same 24-hour day), so the estimate at the depletion moment is exactly zero. No iteration, no floating point. A zero stock gives `recordedAt`. The local date is `depletionInstant.atZone(zone).toLocalDate()`.

The time arithmetic belongs with `Stock` (it already owns elapsed-time math); `Product` exposes `depletionDateIn(zone)` and `nextAlertAt(now, zone)` so callers do not reach into `Stock`.

Alternative considered: compute day by day until the estimate reaches zero. Rejected: slower, and still needs the exact boundary.

### Next alert decision order
1. `depletionDate = depletionDateIn(zone)`; `today = now.atZone(zone).toLocalDate()`.
2. If `today` is after `depletionDate`: `None`.
3. `alertAt = ZonedDateTime.of(depletionDate.minusDays(leadTime), 09:00, zone)`.
4. If `now` is before `alertAt.toInstant()`: `Scheduled(alertAt)`; otherwise `Due`.

`ZonedDateTime.of` already implements the clock-change rules in the spec: a local time in a gap is shifted later by the gap length, and in an overlap the earlier offset is used. The 09:00 alert time is a named constant in the domain, not a parameter (fixed by spike `#38`).

### Testing clock changes with a test-only zone
No real time zone is known to change its clocks at 09:00, so the gap and overlap scenarios use test-only zones registered with a `java.time.zone.ZoneRulesProvider` in the domain's test sources (for example `Test/Gap0900` and `Test/Overlap0900`, built with `ZoneRules.of` and one transition each). The daylight-saving-start scenario uses the real `Europe/Madrid`. All tests pass explicit `Instant`s and `ZoneId`s; none depends on the machine's time zone.

## Risks / Trade-offs

- [`Product.create` signature changes] → Only domain tests call it today; they are updated in this change. No persisted data exists yet (`#41`).
- [Floor to milliseconds can move a depletion moment that is within 1 ms after midnight to the previous date] → Negligible for a product that only uses the date; conservative (earlier alert), deterministic, and consistent with the estimate. Coarser rounding (minute, hour, day) was rejected: it adds a rounding step and a choice, local days are not always 24 hours, and the estimate and the date could disagree.
- [`ZoneRulesProvider.registerProvider` is global and can be called only once per JVM per zone id] → Register in one shared test helper guarded so repeated test classes do not register twice.
- [Next alert ignores delivery state, so a delivered alert still reads `Due` until the depletion date passes] → Intended: `#45` combines this with its delivery record. The UI preview (`#42`) must be designed with that in mind.
