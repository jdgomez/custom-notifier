## Purpose

Defines the only path by which a change reaches the main branch (gate, pull request, green CI, owner squash-merge) and what the repository and the gate enforce along that path.

## ADDED Requirements

### Requirement: Main accepts changes only through pull requests
The `main` branch SHALL reject direct pushes, force pushes and deletion for every account, including repository administrators.

#### Scenario: Direct push attempt
- **GIVEN** the `main` ruleset is active
- **WHEN** any account, including the owner's, pushes a commit directly to `main`
- **THEN** GitHub rejects the push

### Requirement: Merge requires green, up-to-date CI
A pull request into `main` SHALL be mergeable only when the `verify` and `e2e` checks have passed on its latest commit, and its branch is up to date with `main`.

#### Scenario: Failing check blocks merge
- **GIVEN** a pull request whose `e2e` check failed
- **WHEN** the owner tries to merge it
- **THEN** GitHub refuses the merge

#### Scenario: Stale branch blocks merge
- **GIVEN** a green pull request whose branch is behind `main`
- **WHEN** the owner tries to merge it
- **THEN** GitHub requires the branch to be updated and the checks to pass again first

### Requirement: Squash-only merges with clean history
The repository SHALL allow only squash merges, SHALL use the pull request title as the squash commit title, SHALL keep `main`'s history linear, SHALL delete the head branch after merge, and SHALL NOT allow auto-merge.

#### Scenario: Merge a change
- **GIVEN** a green pull request titled with a Conventional Commits title
- **WHEN** the owner merges it
- **THEN** exactly one commit with that title lands on `main`, and the `change/<name>` branch is deleted

### Requirement: Gate runs the project's checks
The no-mistakes gate SHALL run the project's documented lint and JVM test commands, and its CI step SHALL wait for the `verify` and `e2e` checks of the pull request it opens.

#### Scenario: Gate on a change with a lint finding
- **GIVEN** a committed change that introduces a lint finding
- **WHEN** the reviewer runs the gate
- **THEN** the gate's lint step reports the finding from the project's lint command before any pull request is opened

#### Scenario: Gate waits for CI
- **GIVEN** a change that passes the gate's local steps
- **WHEN** the gate opens the pull request
- **THEN** the gate's CI step finishes only after `verify` and `e2e` report their results

### Requirement: Pull requests follow one template
Every pull request SHALL be created from the repository template, which asks for a Conventional Commits title, a `Closes #N` line, a link to the OpenSpec change, test evidence, and, for user-visible changes, screenshots and a link to the E2E video.

#### Scenario: New pull request body
- **GIVEN** the template is committed on `main`
- **WHEN** a pull request is opened
- **THEN** its body starts from the template's sections
