# depletion-and-alert Specification

## Purpose
Defines when a product runs out and when the user must be alerted about it: the rule and its lead time, the depletion date and the next alert, always as local dates and times in an explicitly given time zone.

## Requirements

### Requirement: Rule and lead time
Each product SHALL have exactly one rule. The rule SHALL hold a lead time: a whole number of days from 0 to 365, inclusive, meaning how many days before the depletion date the alert fires. A lead time of 0 SHALL mean the alert fires on the depletion date itself. A lead time outside that range SHALL be rejected with an error naming the lead time, and no product SHALL be created or changed.

#### Scenario: Range limits are accepted
- **GIVEN** a lead time of 0 days or of 365 days
- **WHEN** a rule is created with it
- **THEN** the rule exists with that lead time

#### Scenario: Out-of-range lead time
- **GIVEN** a lead time of -1 days or of 366 days
- **WHEN** a rule is created with it
- **THEN** it is rejected with an error naming the lead time

### Requirement: Editing the rule
The user SHALL be able to replace a product's rule with another valid rule. Changing the rule SHALL NOT change the stock or the depletion date; it SHALL change only the next alert.

#### Scenario: Lead time change
- **GIVEN** a product with an estimated stock of 30 units now, a depletion date of 2026-03-31 and a lead time of 10 days
- **WHEN** the lead time is changed to 5 days
- **THEN** the estimated stock now is still 30 units, the depletion date is still 2026-03-31, and the next alert is on 2026-03-26

### Requirement: Depletion date
The depletion moment of a product SHALL be the exact moment at which its estimated stock reaches zero: the moment the stock was last recorded plus the recorded units divided by the consumption rate, where a day is 24 hours of elapsed time. The depletion date SHALL be the local date of the depletion moment in a time zone given explicitly by the caller; the system default time zone SHALL NOT be used implicitly. The depletion date SHALL depend only on the stock, the consumption rate and the time zone, not on the current moment, and SHALL be recalculated from them whenever it is needed, never stored. A product whose recorded stock is zero SHALL have its depletion moment at the moment of that recording.

#### Scenario: Whole days
- **GIVEN** a product with a stock of 10 units recorded at 2026-03-01 12:00 in Europe/Madrid and a consumption rate of 1 unit every 1 day
- **WHEN** its depletion date is computed in Europe/Madrid
- **THEN** it is 2026-03-11

#### Scenario: Fractional rate
- **GIVEN** a product with a stock of 10 units recorded at 2026-03-01 12:00 in Europe/Madrid and a consumption rate of 3 units every 2 days
- **WHEN** its depletion date is computed in Europe/Madrid
- **THEN** its depletion moment is 2026-03-08 04:00 in Europe/Madrid (6 days and 16 hours later) and its depletion date is 2026-03-08

#### Scenario: Time zone decides the date
- **GIVEN** a product whose depletion moment is 2026-06-02 20:00 UTC
- **WHEN** its depletion date is computed in Europe/Madrid and in Asia/Tokyo
- **THEN** it is 2026-06-02 in Europe/Madrid and 2026-06-03 in Asia/Tokyo

#### Scenario: Depletion at midnight
- **GIVEN** a product with a stock of 1 unit recorded at 2026-03-10 00:00 in Europe/Madrid and a consumption rate of 1 unit every 1 day
- **WHEN** its depletion date is computed in Europe/Madrid
- **THEN** it is 2026-03-11

#### Scenario: Already out of stock
- **GIVEN** a product created with a stock of 0 units at 2026-03-10 18:00 in Europe/Madrid
- **WHEN** its depletion date is computed in Europe/Madrid at any later moment
- **THEN** it is 2026-03-10

#### Scenario: Adjustment moves the depletion date
- **GIVEN** a product with a stock of 10 units recorded at 2026-03-01 12:00 in Europe/Madrid, a consumption rate of 1 unit every 1 day, and so a depletion date of 2026-03-11
- **WHEN** the user restocks 1 package of 30 units at 2026-03-01 12:00
- **THEN** its depletion date in Europe/Madrid is 2026-04-10

### Requirement: Next alert
The alert moment of a product SHALL be 09:00 local time, in the time zone given explicitly by the caller, on the date that is the lead time in days before the depletion date. Given a current moment, the next alert SHALL be exactly one of:
- **Scheduled** at the alert moment, when the current moment is before the alert moment;
- **Due**, meaning it is to be delivered now, late, when the alert moment has been reached or passed and the local date of the current moment is not after the depletion date;
- **None**, when the local date of the current moment is after the depletion date.
Whether an alert has already been delivered SHALL NOT affect the next alert in this capability.

#### Scenario: Scheduled
- **GIVEN** a product with a depletion date of 2026-03-11 and a lead time of 10 days, in Europe/Madrid
- **WHEN** its next alert is computed at 2026-02-20 10:00 in Europe/Madrid
- **THEN** it is scheduled at 2026-03-01 09:00 in Europe/Madrid

#### Scenario: Reaching the alert moment
- **GIVEN** a product with a depletion date of 2026-03-11 and a lead time of 10 days, in Europe/Madrid
- **WHEN** its next alert is computed at exactly 2026-03-01 09:00 in Europe/Madrid
- **THEN** it is due

#### Scenario: Missed alert is delivered late until the depletion date
- **GIVEN** a product with a depletion date of 2026-03-11 and a lead time of 10 days, in Europe/Madrid
- **WHEN** its next alert is computed at 2026-03-05 10:00 or at 2026-03-11 23:59 in Europe/Madrid
- **THEN** it is due

#### Scenario: After the depletion date
- **GIVEN** a product with a depletion date of 2026-03-11 and a lead time of 10 days, in Europe/Madrid
- **WHEN** its next alert is computed at 2026-03-12 00:00 in Europe/Madrid
- **THEN** there is none

#### Scenario: Lead time of zero
- **GIVEN** a product whose depletion moment is 2026-03-11 03:00 in Europe/Madrid and whose lead time is 0 days
- **WHEN** its next alert is computed at 2026-03-11 08:00 and at 2026-03-11 10:00 in Europe/Madrid
- **THEN** it is scheduled at 2026-03-11 09:00 in Europe/Madrid at 08:00, and due at 10:00

#### Scenario: Lead time longer than the remaining stock
- **GIVEN** a product created at 2026-03-01 12:00 in Europe/Madrid with a stock of 5 units, a consumption rate of 1 unit every 1 day and a lead time of 10 days
- **WHEN** its next alert is computed at 2026-03-01 12:00 in Europe/Madrid
- **THEN** it is due

#### Scenario: Time zone decides the alert moment
- **GIVEN** a product with a depletion date of 2026-06-11 in a given time zone and a lead time of 10 days
- **WHEN** its next alert is computed at 2026-05-01 00:00 UTC in Europe/Madrid and in America/New_York
- **THEN** it is scheduled at 2026-06-01 09:00 local time in each zone, which is 07:00 UTC in Europe/Madrid and 13:00 UTC in America/New_York

### Requirement: Alert time on clock changes
The alert moment SHALL stay at 09:00 local time on days when the time zone's offset changes. When 09:00 does not exist on the alert date because the clocks jump forward over it, the alert moment SHALL be shifted later by the length of the gap. When 09:00 occurs twice because the clocks go back over it, the alert moment SHALL be the earlier of the two.

#### Scenario: Daylight saving time starts
- **GIVEN** a product whose alert date is 2026-03-29, the day daylight saving time starts in Europe/Madrid
- **WHEN** its next alert is computed in Europe/Madrid before that date
- **THEN** it is scheduled at 2026-03-29 09:00 in Europe/Madrid, which is 07:00 UTC

#### Scenario: 09:00 falls in a gap
- **GIVEN** a time zone whose clocks jump from 09:00 to 10:00 on the alert date
- **WHEN** the next alert is computed in it before that date
- **THEN** it is scheduled at 10:00 local time on the alert date

#### Scenario: 09:00 occurs twice
- **GIVEN** a time zone whose clocks go back from 10:00 to 09:00 on the alert date
- **WHEN** the next alert is computed in it before that date
- **THEN** it is scheduled at the first 09:00 local time on the alert date
