## 1. Rule

- [x] 1.1 Add `LeadTime` (whole days, `0..365`) and `Rule`, with tests for the "Rule and lead time" scenarios (0 and 365 accepted, -1 and 366 rejected naming the lead time)
- [x] 1.2 Add `rule` to `Product`: required by `Product.create`, editable with `changeRule` without touching the stock; update the existing domain tests to the new signature and cover the modified "Product definition" scenarios (valid product with a lead time, invalid rule)

## 2. Depletion date

- [x] 2.1 Add the exact depletion moment (closed form from the stock anchor and the rate, floored to milliseconds; zero stock = recording moment) and `DepletionDate` in an explicit `ZoneId`, exposed on `Product`, with tests for every "Depletion date" scenario (whole days, fractional rate, time zone decides the date, midnight, already out of stock, adjustment moves the date)
- [x] 2.2 Add a test that the estimated stock at the depletion moment is exactly zero, for a fractional rate

## 3. Next alert

- [x] 3.1 Add `NextAlert` (`Scheduled` with the zoned alert time, `Due`, `None`) and its computation at 09:00 local on the depletion date minus the lead time, exposed on `Product`, with tests for every "Next alert" scenario and the "Editing the rule" scenario
- [x] 3.2 Add test-only time zones with a gap and an overlap at 09:00 (registered once through a `ZoneRulesProvider` in test sources) and tests for every "Alert time on clock changes" scenario, including daylight saving time start in `Europe/Madrid`; no test depends on the machine's time zone

## 4. Verification

- [x] 4.1 Run `./gradlew lintAll test` with zero warnings, and confirm production code stays within about 400 lines
- [x] 4.2 Update the "Project state" section of `AGENTS.md` if the change affects it, mark every task done and keep `design.md` in sync with any deviation decided during implementation
