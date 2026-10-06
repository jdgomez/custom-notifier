## Context

The `:domain` module exists (pure Kotlin/JVM, JUnit 4 and `kotlin-test` for tests, warnings as errors) but has no sources yet. This change adds its first types. Requirements are in `specs/product-stock/spec.md`; motivation in `proposal.md`. Glossary terms in `docs/SESSION0.md` are the type names (ADR 0012, tactical DDD).

## Goals / Non-Goals

**Goals:**
- Immutable, self-validating domain types that make invalid products unrepresentable.
- Exact, deterministic stock estimation that later changes (depletion date) can reuse without re-deriving it.

**Non-Goals:**
- Product identity: assigned when persistence is designed (change 4), not invented here.
- Local dates and time zones (change 3).

## Decisions

### Types follow the glossary, one value type per concept
Package `dev.jdgomez.customnotifier.domain` (or a `product` subpackage if the executor finds it clearer). Proposed types:
- `Product`: immutable entity holding the values below; every operation returns a new `Product`.
- `ProductName`, `UnitLabel`: trimmed, non-blank text. The glossary term is "Unit", but `Unit` is Kotlin's built-in type, so the type is `UnitLabel`; this is the only deliberate deviation from the glossary.
- `PackageSize`: positive `Int` of units.
- `ConsumptionRate`: `units` and `days`, both positive `Int`.
- `Stock`: the recorded stock, an exact quantity of units plus the `Instant` it was recorded at.
- `EstimatedStock`: the exact estimate at a moment, plus its whole-unit view (floored) and whether it was rounded.
- `Adjustment` (non-zero `Int` units) and `Restock` (packages, `Int` >= 1) may be value types or operation parameters; the executor chooses, as long as invalid values cannot be applied.

Alternative considered: bare `Int`/`String` fields with validation in `Product`. Rejected: ADR 0012 asks for dedicated types, and later changes (UI, persistence) get validation for free.

### Validation by exceptions at construction
Constructors (or factory functions) throw `IllegalArgumentException` (via `require`) with a message naming the field. The UI change will validate input before building the types and map these to messages. Alternative considered: a `Result`/sealed-error return type. Rejected for now: no caller needs to branch on errors yet; it can be introduced when the UI change needs it, without changing the specs.

### Stock is anchored; every stock-changing event re-anchors
`Stock` = (exact units, recorded at). The estimate at `now` is `max(0, units - rate * elapsed)`. Adjustment, restock and rate change compute the estimate at their moment, apply their effect, and store the result as the new `Stock` anchored at that moment. Package size, name and unit label edits do not touch `Stock`. This makes the history irrelevant: only the last anchor and the current rate matter, which is also all persistence will have to store.

Alternative considered: storing an event log and replaying it. Rejected: more storage and complexity for no MVP need.

### Time is an input, not a dependency
Domain operations take `now: Instant` as a parameter. The app injects a `java.time.Clock` and passes `clock.instant()`. This keeps the domain deterministic and tests free of fakes. Elapsed time is measured in whole milliseconds; a negative elapsed time (clock moved backwards) counts as zero. A day is exactly 86,400,000 ms.

### Exact arithmetic with a rational of `BigInteger`
The exact quantity of units is a reduced fraction of `BigInteger` numerator and denominator (internal type, e.g. `Quantity`). Consumption over `e` ms is `units * e / (days * 86_400_000)`, which is exact; repeated re-anchoring with different rates keeps it exact and cannot overflow. Alternatives considered: `Double` (rejected: spec forbids floating-point rounding); `BigDecimal` (rejected: 1/3 has no finite decimal, so a scale and rounding mode would be needed); `Long` fractions (rejected: denominators grow with re-anchoring and could overflow). Whole units view = floor of the fraction; "rounded" = denominator is not 1 after reduction.

The required test of the exactness: 3 units every 2 days after exactly 1 day consumes exactly 3/2 units.

## Risks / Trade-offs

- [Fraction denominators can grow with many re-anchors at different rates] -> Bounded in practice (one denominator factor per rate change, at most days * 86,400,000); `BigInteger` removes the overflow risk. Persistence (change 4) must store numerator and denominator, or an equivalent exact form; note it there.
- [Millisecond truncation of elapsed time] -> At most 1 ms of consumption lost per estimate, far below one unit; deterministic for a given pair of instants.
- [Exceptions for validation may be awkward for the UI] -> Revisit in the UI change if needed; the specs only require rejection naming the field.
