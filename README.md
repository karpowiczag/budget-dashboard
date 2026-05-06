# Budget Dashboard

[![CI](https://github.com/karpowiczag/budget-dashboard/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/karpowiczag/budget-dashboard/actions/workflows/ci.yml)
[![UI Smoke](https://github.com/karpowiczag/budget-dashboard/actions/workflows/ui-smoke.yml/badge.svg)](https://github.com/karpowiczag/budget-dashboard/actions/workflows/ui-smoke.yml)
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

The no-card deployment target is a Render Free Web Service backed by Neon Free Postgres. Configure these environment variables on the Render service:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgresql://...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
APP_ALLOWED_GOOGLE_EMAIL=you@example.com
APP_LOCAL_REBUILD_ENABLED=false
```

Render provides `PORT` at runtime; the app falls back to `8080` for local containers. Use the Neon pooled PostgreSQL connection string when available. Configure this Google OAuth redirect URI for the deployed app:

```text
https://<render-app>.onrender.com/login/oauth2/code/google
```

The raw CSV upload is processed in memory and not retained. The app persists normalized transactions, import audit records, budget settings, and structured analytics snapshots in PostgreSQL.

## CI/CD

GitHub Actions:

- `ci.yml` tests and builds React from `frontend/`, syncs the frontend into Spring static resources, and runs Java tests for every PR into `develop`/`main` and every push to those branches. It cancels older in-progress runs on the same branch.
- `ui-smoke.yml` is manual-only. It starts Spring Boot with an isolated H2 database, imports committed fake CSV data through the browser, and checks the desktop dashboard workflow with Playwright.
- `deploy.yml` is manual-only and runs only from `main` because the GraalVM native build is expensive. It builds the native binary, packages a minimal Docker image, pushes it to GHCR, deploys the selected image tag with Render CLI `--wait`, and smokes `/actuator/health`. `RENDER_API_KEY`, `RENDER_SERVICE_ID`, and `RENDER_PUBLIC_URL` are required GitHub secrets for a release deployment. Production database and OAuth values are configured as Render service environment variables, not stored in the repository.
- Dependabot checks npm, Maven, GitHub Actions, and Docker weekly against `develop`, grouped by ecosystem with major version updates ignored so dependency maintenance does not burn CI minutes unexpectedly.
- GitHub branch protection is the merge gate for `develop` and `main`. It requires PRs, the `test` status check, up-to-date branches, resolved review conversations, dismisses stale reviews, includes admins, and blocks force pushes/deletions.

Create the Render service as an image-backed Web Service using `ghcr.io/karpowiczag/budget-dashboard/budget:latest` as the default image URL, choose the Free instance type, and set `/actuator/health` as the health check path. Make the GHCR package public or configure Render registry credentials before the first deploy. Use Neon for PostgreSQL instead of Render Free Postgres because Render Free Postgres expires after 30 days.

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
