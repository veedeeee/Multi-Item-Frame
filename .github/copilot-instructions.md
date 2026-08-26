
# Copilot Instructions

## Project Overview
- The user-facing overview is in the `README.md`. The assistant should read that file to understand the project and its goals.

## Mod features and dependencies
- This mod has no required dependencies.
- If the player also installed Applied Energistics 2, the mod will support the copying and pasting the Item Frame settings with `ae2:memory_card`
- If the player also installed Mekanism, the mod will support the copying and pasting the Item Frame settings with `mekanism:configuration_card`
- If the player also installed Just Enough Items (JEI), this mod allows to choose the showing item from the JEI interface.

## Development Commands
- Build all modules: `.\gradlew.bat build --console=plain`
- Compile common + NeoForge only: `.\gradlew.bat :common:compileJava :neoforge:compileJava --console=plain`
- Run NeoForge client: `.\gradlew.bat :neoforge:runClient --console=plain`
- Run Forge client: `.\gradlew.bat :forge:runClient --console=plain`

## Interactive App Launch Rule
When a member runs `:neoforge:runClient`, `:forge:runClient`, or any `*runClient` command,
that member MUST output a Japanese operator guidance message on console before returning
control. The message should tell the user: what to do now in the launched game client,
what to check/test, and when to close the client and report results back.

## Git Operation Rules
- Default branch (`main` or `master`) is protected. No one can push directly to it. All changes must be made via PRs.
- The git-flow is used for development. Only `main`/`master`, `develop`, and `release/*` are required
  branches; topic branches (`feature/*`, `fix/*`, `chore/*`, etc.) are optional — the assistant
  and the user may work directly on `develop` instead of cutting one for every change. A
  `release/vX.Y.Z` branch is still required when preparing a PR into `main` (see Release Flow
  below).
- The assistant needs to use the **`.github/workflows/create-pr.yml`** workflow (`gh workflow run create-pr.yml -f
  head=<branch> -f base=<target> -f title=<title> -f body=<body>`) to open bot-authored PRs
  instead of `gh pr create`. `base` defaults to `main` but accepts any target branch (e.g.
  `develop`), so the same workflow covers both release PRs and regular feature/fix PRs into
  `develop`.
  - Arguments notes:
    - `head`: the source branch (e.g., `feature/xyz` or `release/v1.2.3`)
    - `base`: (Optional, default=`main`) the target branch (e.g., `develop` or `main`)
    - `title`: the PR title
    - `body`: (Optional) the PR body
  - The reason why `gh pr create` is not used is that it opens PRs from the repository owner's account, which
    makes it impossible to review the PR before merging. The workflow opens PRs from the bot
    account, which allows normal reviewability.

### Release Flow (MOD-specific)
- Version manifest: `gradle.properties` — update the `version=` field.
- Build command before PR: `.\gradlew.bat build --console=plain`

- Build modules: `common`, `forge`, `neoforge`
- Artifact paths after build:
  - Forge: `forge/build/libs/`
  - NeoForge (1.21.1): `neoforge/build/libs/`
- Branch flow: feature/fix branches merge into `develop` first. A `release/vX.Y.Z` branch is then
  cut from `develop` for the version bump + changelog commit, and that branch is what gets PR'd
  into `main`.
- User test checklist: use `docs/user-test-checklist-template.md` as the source. Copy its body
  into the `release/vX.Y.Z` -> `main` PR description (filling in the fixed items for that
  release) rather than committing a version-specific checklist file — the template itself is
  the only checklist file tracked in the repository.
  - If the template document is not existing or it looks outdated, create or update it accordingly.

## Coding Conventions
- Follow the `.editorconfig` file in this repository as the source of truth for code
  formatting rules. Do not hard-code formatting rules in instruction files.

## Supported Loaders
- NeoForge 1.21.1
- Forge 1.20.1

### Test Environments
- `D:\curseforge\minecraft\Instances`
  - `MultiIF-Forge 1.20.1\`
  - `MultiIF-NeoForge 1.21.1\`
