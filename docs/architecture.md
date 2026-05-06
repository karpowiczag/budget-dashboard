# Architecture Notes

The backend follows a lightweight layered structure:

- `domain`: immutable records that represent bank transactions, normalized transactions, imports, and report results.
- `application`: use cases, analysis services, categorization chain, and persistence ports.
- `infrastructure`: CSV reader, Spring Data JDBC entities, repository interfaces, and persistence adapters.
- `web`: REST controllers and API exception mapping.
- `config`: Spring Boot configuration properties, security, and data source wiring.

Current design choices:

- Categorization uses a Chain of Responsibility. Regex rules run first, positive-flow fallback runs second, and manual-review fallback runs last.
- Transaction normalization is separated from annual analysis, so classification confidence and excluded-flow logic can be tested independently.
- Application services depend on `BudgetReportStore`, not concrete JDBC classes.
- Persistence uses Spring Data JDBC repositories and `JdbcAggregateTemplate` for aggregate inserts with assigned report-year IDs.
- Flyway migrations remain the source of truth for database shape.
- Google OAuth/OIDC is used in production and only one verified Google email is allowlisted.
- OAuth sessions use cookie-backed CSRF protection for state-changing browser requests.
- `KnownCsvEndToEndIntegrationTest` runs against local ignored `2025/` and `2026/` CSV exports when they exist, without committing bank data or exact private totals.

Next refactor targets:

- Split `BudgetAnalysisService` further into monthly, category, recurring, and recommendation analyzers.
- Move Polish presentation labels out of `NormalizedTransaction.toPayloadMap()` into a web mapper.
- Split the React app into feature folders and introduce TypeScript API types.
