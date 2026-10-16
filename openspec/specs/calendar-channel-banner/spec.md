# calendar-channel-banner Specification

## Purpose
TBD - created by archiving change add-calendar-channel-banner. Update Purpose after archive.

## Requirements

### Requirement: Calendar channel banner
While the calendar channel is on, at least one product exists, and the channel cannot work, the product list SHALL show, above the products, a banner with the button "Fix" and one of these texts:
- "Calendar alerts are off. Calendar access was removed." when the app does not have calendar access (read and write);
- "Calendar alerts are off. The chosen calendar is no longer available." when access is granted but the chosen calendar no longer exists or is no longer writable.
Tapping "Fix" SHALL open the Settings screen. The banner SHALL NOT be dismissible. It SHALL be checked again every time the product list becomes visible, and SHALL disappear as soon as the channel can work again or is turned off. With no products, or with the channel off, the banner SHALL NOT be shown. When the disabled-alerts banner is shown too, both SHALL be shown, the disabled-alerts banner first. The banner SHALL stay readable at the largest font size and in light and dark theme, with the button beside the text or, when the text needs more than two lines beside it, below the text, like the disabled-alerts banner.

#### Scenario: Access removed
- **GIVEN** the calendar channel on with "Home", a product "Coffee", and calendar access revoked in the system settings
- **WHEN** the user opens the app
- **THEN** the product list shows the banner "Calendar alerts are off. Calendar access was removed." with the button "Fix"

#### Scenario: Calendar gone
- **GIVEN** the calendar channel on with "Home", a product "Coffee", calendar access granted, and "Home" removed from the device
- **WHEN** the product list becomes visible
- **THEN** it shows the banner "Calendar alerts are off. The chosen calendar is no longer available." with the button "Fix"

#### Scenario: Fix opens Settings
- **GIVEN** the calendar channel banner on the product list
- **WHEN** the user taps "Fix"
- **THEN** the Settings screen is shown

#### Scenario: Banner disappears after the fix
- **GIVEN** the banner "Calendar alerts are off. Calendar access was removed."
- **WHEN** the user taps "Fix", taps "Allow" in Settings, allows calendar access and goes back
- **THEN** the product list shows no calendar channel banner

#### Scenario: Turning the channel off removes the banner
- **GIVEN** the banner "Calendar alerts are off. The chosen calendar is no longer available."
- **WHEN** the user taps "Fix", turns the switch off and goes back
- **THEN** the product list shows no calendar channel banner

#### Scenario: No banner while the channel works or is off
- **GIVEN** a product "Coffee" and either the channel off, or the channel on with access granted and the chosen calendar usable
- **WHEN** the product list becomes visible
- **THEN** no calendar channel banner is shown

#### Scenario: No banner without products
- **GIVEN** the calendar channel on, calendar access revoked, and no product
- **WHEN** the product list becomes visible
- **THEN** no calendar channel banner is shown

#### Scenario: Both banners
- **GIVEN** a product "Coffee", notifications turned off for the app, and the calendar channel on with calendar access revoked
- **WHEN** the product list becomes visible
- **THEN** the disabled-alerts banner is shown first and the calendar channel banner below it
