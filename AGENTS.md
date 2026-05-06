# Codex Instructions

These instructions apply to the whole repository.

## Project Context

This is a private household budget dashboard. It imports bank CSV exports, classifies transactions, stores normalized data in PostgreSQL/H2 through Spring Data JDBC, and serves a React dashboard from Spring Boot.

The default branch is `develop`. Production releases flow from `develop` to `main`.

## Data Safety

- Never commit bank exports, generated reports, local databases, or secrets.
- Treat `2025/`, `2026/`, `20[0-9][0-9]/`, `data/`, `outputs/`, `*.csv`, `*.xlsx`, and `.env*` as private local data.
- Do not paste private transaction rows into issues, PRs, commits, or logs.
- Tests based on private CSV folders must assert structural invariants only, not exact private totals.

## Branch And PR Flow

Use the solo GitFlow process:

1. Start from a clean `develop`.
2. Create a branch named `feature/<short-name>`, `fix/<short-name>`, `refactor/<short-name>`, or `hotfix/<short-name>`.
3. Link every PR to a GitHub issue when practical.
4. Open PRs into `develop`, not directly into `main`.
5. Wait for CI before merge.
6. Squash merge feature/fix/refactor PRs and delete the branch.
7. Release by merging `develop` into `main` and running the manual deploy workflow.

Do not commit directly to `develop` or `main` unless the user explicitly asks for a direct commit.

## Architecture Rules

- `domain` contains immutable business records and must not import `application`, `infrastructure`, `web`, or `config`.
- `application` contains use cases, analysis, categorization, and ports. It must not import `infrastructure`, `web`, or `config`.
- `infrastructure` contains adapters such as CSV parsing and Spring Data JDBC persistence.
- `web` contains REST/SPA controllers and API error mapping.
- `config` wires Spring configuration, security, data source, and adapter beans.
- Prefer Spring Data JDBC over JPA for persistence in this app.
- Keep bank-category corrections title-driven and deterministic unless the user explicitly asks for ML/external enrichment.

## Test Commands

Use Java 25. On Windows local machines, this repo currently uses:

```powershell
$env:JAVA_HOME='C:\Users\sanyak\.jdks\openjdk-25.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Before opening or updating a PR, run:

```powershell
npm run build
npm run sync:frontend
.\mvnw.cmd test
```

Native/GraalVM builds are expensive. Do not run `native:compile` locally or in normal CI unless the user explicitly requests a native release validation.

## Review Expectations

When reviewing a PR, focus on:

- Incorrect budget totals, false-positive income/spend classification, and transfer handling.
- Security and privacy regressions around OAuth, CSV upload, secrets, and private bank data.
- Package boundary violations and unnecessary coupling.
- CI/free-tier cost impact.
- Missing tests for categorization, CSV parsing, analytics, persistence, and security behavior.
- UI regressions for desktop dashboard workflows.

If a review finds issues, fix them on the same PR branch, rerun tests, and update the PR.
