# Budget Dashboard

[![CI](https://github.com/karpowiczag/budget-dashboard/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/karpowiczag/budget-dashboard/actions/workflows/ci.yml)
[![Deploy](https://github.com/karpowiczag/budget-dashboard/actions/workflows/deploy.yml/badge.svg?branch=main)](https://github.com/karpowiczag/budget-dashboard/actions/workflows/deploy.yml)

Household budget dashboard for recurring bank CSV exports. Source code is public; bank exports, generated reports, local databases, and secrets stay private and must never be committed.

## Stack

- React + Vite frontend
- Spring Boot 4 backend
- Java 25 LTS
- GraalVM native image for production
- Spring Data JDBC repositories over PostgreSQL in production and local H2 for development
- Google OAuth in production, allowlisted to one verified Google email

Java 25 usage is documented in [docs/java-25.md](docs/java-25.md).
Current backend architecture is documented in [docs/architecture.md](docs/architecture.md).
Codex-assisted issue, PR, review, and merge workflow is documented in [docs/codex-gitflow.md](docs/codex-gitflow.md).

## Repository Layout

- `frontend/` contains the React/Vite app, npm dependencies, and frontend build scripts.
- `src/main/java`, `src/main/resources`, and `src/test` contain the Spring Boot backend.
- `frontend/dist` is copied into `src/main/resources/static` only as a generated deployment artifact.

## Local Development

Install frontend dependencies:

```powershell
npm --prefix frontend install
```

Run the frontend only:

```powershell
npm --prefix frontend run dev
```

Build the frontend and copy it into Spring static resources:

```powershell
npm --prefix frontend run build
npm --prefix frontend run sync:backend
```

Run the Spring app:

```powershell
.\mvnw.cmd spring-boot:run
```

This project targets Java 25. On Windows, verify the shell before running Maven:

```powershell
java -version
$env:JAVA_HOME="C:\Users\sanyak\.jdks\openjdk-25.0.1"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

By default local auth is disabled and the app uses `data/budget.mv.db`. Upload a bank CSV from the Import tab or rebuild local year folders:

```powershell
Invoke-RestMethod -Method Post http://127.0.0.1:8080/api/v1/imports/rebuild
```

Household-specific categorization rules should stay out of public source. Put private salary/counterparty rules in ignored local Spring config such as `application-local.properties`:

```properties
app.categorization.personal-rules[0].pattern=PRIVATE EMPLOYER.*WYNAGRODZENIE
app.categorization.personal-rules[0].category=Pensja
```

## Production Settings

Required Koyeb environment variables:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgres://...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
APP_ALLOWED_GOOGLE_EMAIL=you@example.com
PORT=8080
```

The raw CSV upload is processed in memory and not retained. The app persists normalized transactions, import audit records, budget settings, and structured analytics snapshots in PostgreSQL.

## CI/CD

GitHub Actions:

- `ci.yml` tests and builds React from `frontend/`, syncs the frontend into Spring static resources, and runs Java tests for every PR into `develop`/`main` and every push to those branches. It cancels older in-progress runs on the same branch.
- `deploy.yml` is manual-only and runs only from `main` because the GraalVM native build is expensive. It builds the native binary, packages a minimal Docker image, pushes it to GHCR, creates or updates Koyeb, and smokes `/actuator/health`. `KOYEB_TOKEN`, `KOYEB_APP`, `KOYEB_SERVICE`, and `KOYEB_PUBLIC_URL` are required for a release deployment.
- Dependabot checks npm, Maven, GitHub Actions, and Docker weekly against `develop`, grouped by ecosystem with major version updates ignored so dependency maintenance does not burn CI minutes unexpectedly.
- GitHub branch protection is the merge gate for `develop` and `main`. It requires PRs, the `test` status check, up-to-date branches, resolved review conversations, dismisses stale reviews, includes admins, and blocks force pushes/deletions.

For private GHCR images, create a Koyeb private-registry secret and expose its name to GitHub Actions as `KOYEB_GHCR_SECRET`. Production database and OAuth values should be configured directly in Koyeb secrets/environment variables.

## Solo GitFlow

Use `main` as production and `develop` as the daily working branch. Feature branches are optional, but useful for risky work:

```powershell
git switch develop
git switch -c feature/short-name
```

Merge finished work into `develop` only after GitHub branch protection allows the PR to merge. When ready to release, open a PR from `develop` into `main`, tag after merge, push tags, then manually run the Deploy workflow from `main`:

```powershell
gh pr create --base main --head develop --title "Release v2026.05.06-1" --body "Release from develop."
gh pr checks --watch
gh pr merge --merge
git switch main
git pull --ff-only origin main
git tag v2026.05.06-1
git push origin v2026.05.06-1
```

For urgent production fixes, branch from `main` as `hotfix/short-name`, merge it back into both `main` and `develop`.

Keep financial data private even though the source repo is public. `.gitignore` excludes yearly folders, CSV files, generated Excel/JSON reports, local databases, and secrets.
