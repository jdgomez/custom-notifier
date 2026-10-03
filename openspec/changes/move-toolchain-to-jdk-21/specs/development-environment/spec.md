## MODIFIED Requirements

### Requirement: Java toolchain available
The development machine SHALL provide JDK 21 as the default `java` and `javac` on the `PATH` of any new shell, including shells started inside Herdr panes, and SHALL have no other JDK installed once the migration from JDK 17 is complete.

#### Scenario: Java version check in a fresh shell
- **GIVEN** a new login shell on the development machine
- **WHEN** the user runs `java -version` and `javac -version`
- **THEN** both report major version 21

#### Scenario: Single JDK on the machine
- **GIVEN** the owner has completed the documented JDK 17 removal step
- **WHEN** the user lists the installed Java alternatives
- **THEN** only the JDK 21 installation is listed
