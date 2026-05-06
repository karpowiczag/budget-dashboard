# Java 25 Usage

The backend targets Java 25 through Maven:

- `java.version=25`
- `maven.compiler.release=25`

Code style for this project:

- Use `var` for local variables when the initializer makes the type obvious, especially constructors, repository/query results, stream results with clear names, and try-with-resources.
- Keep explicit primitive and financial totals such as `double spendTotal` and `int activeMonthCount`; those types are part of the business meaning.
- Keep explicit method signatures, records, DTOs, repository contracts, and public API shapes.
- Do not enable preview or incubator features in normal builds. Preview flags complicate Spring AOT, GraalVM native images, CI, and deployment with little value for this budget app.

Java 25 features that fit the app today:

- Stable source/target level 25 for compiler and tooling alignment.
- Modern collection helpers already used by the code, such as `getFirst()` and `getLast()`.
- Records, text blocks, switch expressions, and local variable type inference where they keep the code shorter without hiding domain meaning.

Features intentionally avoided for now:

- Preview/incubator APIs and language features.
- Module import declarations, because this is a normal Spring Boot application, not a compact source-file script.
- Scoped values and structured concurrency, because the app does not have a concurrency problem that justifies adding them.
