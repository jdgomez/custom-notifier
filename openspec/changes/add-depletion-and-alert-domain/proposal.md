## Why

The app's goal is that a product never runs out at home: the user must be told, with enough lead time, when to buy more. The domain already knows the estimated stock of a product (`add-product-stock-domain`), but not when it runs out nor when to alert. Every later Phase 1 change (product list with the next alert preview, notifications, calendar events) needs these two answers from one deterministic place in the domain.

## What Changes

- A product gets a **rule**: one per product, holding a **lead time** of whole days from 0 to 365. The rule is given when the product is created and can be edited later. Editing it does not change the stock.
- The domain computes the **depletion date** of a product: the local date, in a time zone passed explicitly, of the exact moment its estimated stock reaches zero. It is derived from the stock and the consumption rate, never stored. A product whose stock is already zero has its depletion date on the date of its last recording.
- The domain computes the **next alert** of a product at a given moment and time zone: scheduled at 09:00 local time on the depletion date minus the lead time; due (to be delivered late) when that moment has passed but the depletion date has not; none once the depletion date has passed.
- **BREAKING** (domain API only, no persisted data exists yet): creating a product now requires a rule.

## Capabilities

### New Capabilities
- `depletion-and-alert`: the rule and its lead time, the depletion date and the next alert of a product, with explicit local dates and time zones.

### Modified Capabilities
- `product-stock`: the product definition gains a rule, validated like the other fields.

## Non-goals

- Tracking whether an alert was already delivered ("exactly one alert per rule"), scheduling with the OS, rescheduling and reconciling on boot, time or time zone changes: `#45` (notifications).
- Calendar events: `#46`.
- A default lead time and how it is presented: a UI decision for `#43`.
- Persisting the rule: `#41` (Room persistence).
- Choosing the alert time of day: fixed at 09:00 local time by the spike `#38`; not configurable in the MVP.

## Impact

- **Depends on:** `#39` (`add-product-stock-domain`, merged).
- **Touches:** `:domain` module only (production code and its tests). No Android code, no UI, no persistence.
- **New dependencies:** none. Uses `java.time` from the JDK.
- `Product.create` gains a rule parameter; existing domain tests are updated accordingly.
