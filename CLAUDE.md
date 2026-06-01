# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Household budget dashboard. A Spring Boot 4 / Java 25 backend imports bank CSV exports, classifies transactions, stores normalized data in PostgreSQL via Spring Data JDBC, and serves a React/Vite SPA. **The source repo is public; bank exports, generated reports, local databases, and secrets are private and must never be committed** (see Data Safety below). Backend and frontend live in the same repo but are otherwise decoupled — the only coupling point is a build step that copies the built SPA into Spring static resources.

## Commands

Java 25 is required. On Windows, set `JAVA_HOME` before any Maven command:

```powershell
$env:JAVA_HOME='C:\Users\sanyak\.jdks\openjdk-25.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Use `.\mvnw.cmd` on Windows, `./mvnw` on Linux/CI. npm commands run from `frontend/` (there is **no root `package.json`**); from the repo root prefix them with `--prefix frontend`.

**Canonical pre-PR check** (mirrors CI in `.github/workflows/ci.yml` — frontend tests, build, sync, then Java tests):

```powershell
npm --prefix frontend run build
npm --prefix frontend run sync:backend
.\mvnw.cmd test
```

| Task | Command |
| --- | --- |
| Frontend dev server | `npm --prefix frontend run dev` |
| All frontend tests | `npm --prefix frontend test` (vitest run) |
| Single frontend test file | `npm --prefix frontend exec -- vitest run src/app/domain/budgetSelectors.test.js` |
| Frontend test by name | `npm --prefix frontend exec -- vitest run -t "pattern"` |
| Production SPA build | `npm --prefix frontend run build` |
| Copy SPA into Spring static | `npm --prefix frontend run sync:backend` |
| Playwright UI smoke (manual) | `npm --prefix frontend run smoke:ui` |
| All Java tests | `.\mvnw.cmd test` |
| Single Java test class | `.\mvnw.cmd test -Dtest=CategoryClassifierTest` |
| Single Java test method | `.\mvnw.cmd test "-Dtest=CategoryClassifierTest#methodName"` |

**Do not run `native:compile` (GraalVM native build) locally or in normal CI** — it is expensive and reserved for the manual deploy workflow.

### Running the backend locally

OAuth is disabled by default locally (`APP_OAUTH_ENABLED=false`), so no login is needed. The normal local runtime is PostgreSQL via Docker Compose:

```powershell
docker compose up -d postgres
$env:DATABASE_URL="postgresql://budget:budget@localhost:15432/budget?sslmode=disable"
.\mvnw.cmd spring-boot:run
```

H2 is no longer the default local runtime; enable it only for disposable runs with `$env:APP_ALLOW_LOCAL_H2="true"`. To run the whole containerized app (SPA served by Spring on `http://127.0.0.1:8080`): `docker compose --profile app up --build`.

## Architecture

### Backend — layered/hexagonal, boundaries enforced by test

Packages under `com.budget` are named by responsibility first, technology second. The dependency direction is strict and **`PackageBoundaryTest` fails the build on violations**:

- `domain` — immutable business records (Java records). Must not import `application`, `infrastructure`, `web`, or `config`.
- `application` — use cases, analysis, categorization, settings, and **ports** (interfaces like `BudgetReportStore`, `BankTransactionReader`). Must not import `infrastructure`, `web`, or `config`.
- `infrastructure` — adapters: CSV parsing (`infrastructure.csv`), Spring Data JDBC repositories/entities (`infrastructure.persistence.jdbc`).
- `web` — REST/SPA controllers (`web.controller`), API DTO records + mappers (`web.dto`), error mapping (`web.error`).
- `config` — Spring wiring: security, data source, and adapter bean definitions; environment-backed properties (`BudgetProperties`).

Consequences of these boundaries (the patterns that span multiple files):

- **Ports, not adapters.** Application services depend on interfaces; the concrete JDBC/CSV implementations are wired in `config`. When adding a use case, define the port in `application`, implement it in `infrastructure`, wire it in `config`.
- **Boundary-crossing types.** File uploads cross the web→application boundary as `TransactionImportFile` (application code never sees `MultipartFile`). Env-backed config crosses config→application as `ImportSettings`/typed settings, never raw `BudgetProperties`.
- **Categorization is a Chain of Responsibility** (`application.categorization`): regex/rule matchers run first, positive-flow fallback second, manual-review fallback last. Keep corrections title-driven and deterministic — do **not** add ML/external enrichment unless explicitly asked.
- **Persistence:** Spring Data JDBC (not JPA) + `JdbcAggregateTemplate` for aggregate inserts with assigned IDs. **Flyway migrations in `src/main/resources/db/migration` are the source of truth for DB shape** — add a new `V<n>__*.sql` rather than editing existing migrations.

### Contract-first API

`docs/openapi/budget-api.yaml` is the API contract for `/api/v1`. `OpenApiContractTest` reflects over the controllers and asserts every operation, operationId, and response schema matches the spec. **When you change a controller endpoint or DTO, update the OpenAPI YAML in the same change** or the test fails. Controllers return `web.dto` contract records (never domain/application records directly) so the contract can evolve deliberately.

### Frontend — container/presenter with pure selectors

Lives entirely under `frontend/src/app`:

- `api/` — the only place that talks to the backend (fetch adapters). State-changing requests must send the CSRF header: `budgetApi.js` reads the `XSRF-TOKEN` cookie into `X-XSRF-TOKEN` via `csrfHeaders()`. Keep new mutating calls consistent with this.
- `domain/` — **pure functions**: formatters, chart option builders, and budget selectors. Business calculations live here (not in components) and are unit-tested directly.
- `hooks/` — data-loading (`useBudgetData`, React Query) and view-model (`useDashboardModel`) hooks.
- `views/` — one presenter component per dashboard tab (Reports, Transactions, Recurring, SavingsPlan, MonthControl, Import, Fire, Wealth).
- `components/{charts,layout,tables,ui}` — reusable presentation. Charts are ECharts wrappers around `EChart.jsx`.

`App.jsx` composes views and holds local UI state; `main.jsx` is the entrypoint only. Tests are colocated `*.test.{js,jsx}` (vitest + Testing Library, jsdom).

### Security

`SecurityConfig` has two modes. Locally (`oauth-enabled=false`) CSRF is disabled and all requests are permitted. In production, Google OIDC login is required and **`GoogleAccountAllowlist` restricts access to one verified Google email**; CSRF uses a non-HttpOnly cookie repository (paired with the frontend's `X-XSRF-TOKEN` header).

## Data Safety (P1)

- Never commit bank exports, generated reports, local databases, or secrets. Treat `2025/`, `2026/`, `20[0-9][0-9]/`, `data/`, `outputs/`, `*.csv`, `*.xlsx`, and `.env*` as private. `.gitignore` already excludes these — **broad `.gitignore` exceptions that could re-enable private data are a P1 issue.**
- Do not paste private transaction rows into issues, PRs, commits, or logs.
- Tests that read local CSV folders (e.g. `KnownCsvEndToEndIntegrationTest` over `2025/`/`2026/`) must assert **structural invariants only**, never exact private totals. Committed fixtures use fake data under `src/test/resources/fixtures/`.
- Household-specific categorization rules (real salary/counterparty patterns) belong in ignored local config (`application-local.properties`), not in source.

## Branch & PR flow

Solo GitFlow: `develop` is the daily working branch and default PR target; `main` is production. Branch as `feature/`, `fix/`, `refactor/`, or `hotfix/<short-name>`. **Open PRs into `develop`, not `main`** (releases flow via a `develop`→`main` PR, then the manual deploy workflow). Do not commit directly to `develop`/`main` unless explicitly asked. See `AGENTS.md` for the full review checklist and `docs/solo-gitflow.md` for release/tag steps.

## Reference docs

- `docs/architecture.md` — backend/frontend layering and design rationale.
- `AGENTS.md` — repo-wide rules (data safety, branch flow, architecture rules, review focus).
- `docs/java-25.md` — Java 25 usage notes.
- `docs/fire-methodology.md` — assumptions behind the FIRE (`application.fire`) module.
