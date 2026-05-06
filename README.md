# Budget Dashboard

Private household budget dashboard for recurring bank CSV exports.

## Stack

- React + Vite frontend
- Spring Boot 4 backend
- Java 25 LTS
- GraalVM native image for production
- PostgreSQL in production, local H2 file database for development
- GitHub OAuth in production, allowlisted to one GitHub login

## Local Development

Install frontend dependencies:

```powershell
npm install
```

Run the frontend only:

```powershell
npm run dev
```

Build the frontend and copy it into Spring static resources:

```powershell
npm run build
npm run sync:frontend
```

Run the Spring app:

```powershell
npm run app
```

By default local auth is disabled and the app uses `data/budget.mv.db`. Upload a bank CSV from the Import tab or rebuild local year folders:

```powershell
Invoke-RestMethod -Method Post http://127.0.0.1:8080/api/rebuild
```

## Production Settings

Required Koyeb environment variables:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgres://...
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
APP_ALLOWED_GITHUB_LOGIN=your-github-login
PORT=8080
```

The raw CSV upload is processed in memory and not retained. The app persists normalized transactions, import audit records, and dashboard payloads in PostgreSQL.

## CI/CD

GitHub Actions:

- `ci.yml` builds React, syncs the frontend into Spring static resources, and runs Java tests for application-code changes on `develop` and `main`. It cancels older in-progress runs on the same branch.
- `deploy.yml` is manual-only and runs only from `main` because the GraalVM native build is expensive. It builds the native binary, packages a minimal Docker image, pushes it to GHCR, and creates or updates Koyeb when `KOYEB_TOKEN`, `KOYEB_APP`, and `KOYEB_SERVICE` are configured.

For private GHCR images, create a Koyeb private-registry secret and expose its name to GitHub Actions as `KOYEB_GHCR_SECRET`. Production database and OAuth values should be configured directly in Koyeb secrets/environment variables.

## Solo GitFlow

Use `main` as production and `develop` as the daily working branch. Feature branches are optional, but useful for risky work:

```powershell
git switch develop
git switch -c feature/short-name
```

Merge finished work into `develop`. When ready to release, merge `develop` into `main`, tag it, push both branches, then manually run the Deploy workflow from `main`:

```powershell
git switch main
git merge --no-ff develop
git tag v2026.05.06-1
git push origin main develop --tags
```

For urgent production fixes, branch from `main` as `hotfix/short-name`, merge it back into both `main` and `develop`.

Keep the repo private and never commit bank exports. `.gitignore` excludes yearly folders, CSV files, generated Excel/JSON reports, local databases, and secrets.
