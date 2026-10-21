## ADDED Requirements

### Requirement: Top bar readable at large font sizes
The product form's top bar SHALL show its whole title at every system font size without truncation; when the title needs more than one line, the bar SHALL grow to fit it, and the back arrow and the save action SHALL stay fully visible and in their usual positions.

#### Scenario: Spanish edit title at the largest font size
- **GIVEN** the app in Spanish and the system font size set to its largest value
- **WHEN** the user opens a product to edit it
- **THEN** "Editar producto" is fully readable inside the top bar, not overlapping the content, and "Guardar" and the back arrow are fully visible on the same bar

#### Scenario: Default font size
- **GIVEN** the default system font size, in English or Spanish
- **WHEN** the "New product" or "Edit product" form is shown
- **THEN** the title fits on one line and the bar looks as before this change

### Requirement: Inline rows wrap by pairs
Every inline row of the product form that mixes number fields and words SHALL keep each number field together with the word that follows it; when the row does not fit on one line, it SHALL wrap only between these pairs, so no word is left alone on a line.

#### Scenario: Rule row at the largest font size
- **GIVEN** the system font size set to its largest value, in English or Spanish
- **WHEN** the product form is shown with a rule of 2 units every 1 day
- **THEN** the rule row shows "[2] cans every" and "[1] days" (or the Spanish equivalent) as two whole pairs, and no word stands alone on a line

#### Scenario: Rule row at the default font size
- **GIVEN** the default system font size
- **WHEN** the product form is shown
- **THEN** the rule row and the alert row each fit on one line, as before this change
