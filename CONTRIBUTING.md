
# Contributing Rules

## Development Flow
- The default branch (main / master) is always deployable.
- The default branch (main / master) is protected. The PR is only way to change there.
- Any development activities should be done on `develop` branch.
- Once a phase of development is done, any activities for releasing should be done on `release/vX.Y.Z` branch.
  - Releasing actions contain,
    - Version numbering.
    - Updating `CHANGELOG.md`
- When the default branch (main / master) is changed, the sync PR is automatically created and merged into the `develop` so the `develop` is always be updated.

## Coding
- If this repository has `.editorconfig` file, even the AI assistant must follow to formatting rules.

## Commits
- Each commits should have one purpose. Don't include multiple changes into the one commit.
- Commit message should be short and clear description of the change.

## Pull Request
- Keep in English.
- AI Assistant can use `.github/workflows/create-pr.yml` workflow.
  - If you have reviewer role of the repo, then your assistant MUST use this workflow. Because you cannot accept your own PR. Through this workflow, the PR will be created by the bot and you can accept it.
- The copy of `docs/user-test-checklist-template.md` content must be included in the body of the PR.
