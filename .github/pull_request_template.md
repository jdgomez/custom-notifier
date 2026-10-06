## Summary

<!-- What changes and why, in a few lines. -->

Closes #N

## OpenSpec change

`openspec/changes/<name>/`

## Test evidence

<!-- Commands run and their results (lintAll, test, E2E), or the CI check links. -->

## Screenshots / video

<!-- User-visible changes only: screenshots and a link to the CI `e2e-evidence` artifact (video). Write "n/a" otherwise. -->

## Checklist

- [ ] Conventional Commits title
- [ ] Behavior specified in the OpenSpec change before code
- [ ] `./gradlew lintAll test` pass with zero warnings
- [ ] Tests exercise real behavior (a bug fix has a test that failed before)
- [ ] `verify` and `e2e` checks are green
- [ ] ADR added or updated if the decision is significant
