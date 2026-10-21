## MODIFIED Requirements

### Requirement: Least privilege and pinned actions
Workflows SHALL request read-only repository permissions unless a job needs more, with the reason stated in a comment. Every third-party action SHALL be referenced by a full commit SHA. The pull request and `main` checks (`verify`, `e2e`) SHALL use no repository secrets. Only the release workflow MAY use secrets: the release token to open the release pull request, and the upload key secrets in the one job that builds the release bundle; that workflow SHALL never run on pull requests.

#### Scenario: Workflow audit
- **GIVEN** the committed workflow files
- **WHEN** they are inspected
- **THEN** every `uses:` reference is pinned to a 40-character commit SHA, the default token permission is `contents: read`, every wider permission has a comment with its reason, no `secrets.` reference appears outside the release workflow, and the release workflow is not triggered by `pull_request` or `pull_request_target`
