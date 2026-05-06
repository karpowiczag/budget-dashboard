# Architecture Notes

The backend follows a lightweight layered/hexagonal structure. Package names describe responsibility first, then technology:

```text
com.budget
  application.analysis          annual analysis and transaction normalization
  application.categorization    category strategy chain and nested category decisions
  application.importing         CSV import use case, import settings, and input-reader port
  application.reporting         query use case and report-store port
  application.settings          persisted budget assumptions and category-limit settings
  domain.category               immutable category value records
  domain.importjob              import audit model
  domain.report                 report input/result models
  domain.transaction            bank and normalized transaction models
  infrastructure.csv            bank CSV reader adapter
  infrastructure.persistence.jdbc Spring Data JDBC repositories and entities
  web.controller                REST and SPA controllers
  web.error                     API exception mapping
  config                        Spring Boot configuration, security, data source, and adapter beans
```

Frontend follows a container/presenter split with pure selectors:

```text
frontend/src/main.jsx                Vite/React entrypoint only
frontend/src/app/App.jsx             dashboard composition, view selection, and local UI state
frontend/src/app/api                 browser API adapters and CSRF handling
frontend/src/app/domain              pure formatters, chart constants, and budget selectors
frontend/src/app/hooks               data-loading and dashboard view-model hooks
frontend/src/app/components/charts   reusable Recharts wrappers
frontend/src/app/components/layout   shell, tabs, global time filter, KPI strip, footer
frontend/src/app/components/tables   reusable table components
frontend/src/app/components/ui       small UI primitives
frontend/src/app/views               one component per dashboard tab
frontend/src/styles.css              shared app styling
```

Current design choices:

- Categorization uses a Chain of Responsibility. Regex rules run first, positive-flow fallback runs second, and manual-review fallback runs last.
- Transaction normalization is separated from annual analysis, so classification confidence and excluded-flow logic can be tested independently.
- Application services depend on ports such as `BudgetReportStore` and `BankTransactionReader`, not concrete JDBC or CSV adapter classes.
- Savings recommendations read persisted budget settings through an application port, so target spend and category limits are not browser-only state.
- Upload handling crosses the web boundary through `TransactionImportFile`; application code does not depend on `MultipartFile`.
- Environment-backed import limits and local paths cross the config boundary through `ImportSettings`; application code does not depend on `BudgetProperties`.
- Persistence uses Spring Data JDBC repositories and `JdbcAggregateTemplate` for aggregate inserts with assigned report-year IDs.
- Flyway migrations remain the source of truth for database shape.
- Google OAuth/OIDC is used in production and only one verified Google email is allowlisted.
- OAuth sessions use cookie-backed CSRF protection for state-changing browser requests.
- `KnownCsvEndToEndIntegrationTest` runs against local ignored `2025/` and `2026/` CSV exports when they exist, without committing bank data or exact private totals.
- `PackageBoundaryTest` enforces the package boundaries: domain cannot import outer layers, and application cannot import infrastructure, web, or config packages.
- Frontend API access uses an Adapter-style boundary in `frontend/src/app/api`, while dashboard calculations live in pure selector functions and UI tabs are presenter components.
- Household-specific categorization rules are configured outside public source through ignored local/prod settings.

Next refactor targets:

- Split `BudgetAnalysisService` further into monthly, category, recurring, and recommendation analyzers.
- Introduce TypeScript API/view-model types for the React app.
