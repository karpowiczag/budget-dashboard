package com.budget.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.importing.BudgetImportService;
import com.budget.application.importing.TransactionImportFile;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.TransactionQuery;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "app.security.oauth-enabled=false")
class PostgresBudgetPersistenceIntegrationTest {
    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("budget")
            .withUsername("budget")
            .withPassword("budget");

    @Autowired
    private BudgetImportService importService;

    @Autowired
    private BudgetReportStore store;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("app.database.url", () -> "postgres://budget:budget@%s:%d/budget?sslmode=disable"
                .formatted(POSTGRES.getHost(), POSTGRES.getFirstMappedPort()));
    }

    @Test
    void importsReplacesAndQueriesReportOnRealPostgres() throws Exception {
        var first = importFixture();
        var second = importFixture();

        assertThat(first.transactions()).isEqualTo(17);
        assertThat(second.transactions()).isEqualTo(17);
        assertThat(count("budget_transactions", 2026)).isEqualTo(17);
        assertThat(count("report_monthly_summaries", 2026)).isEqualTo(12);
        assertThat(count("report_category_summaries", 2026)).isGreaterThan(5);
        assertThat(count("report_hierarchy_summaries", 2026)).isGreaterThan(5);
        assertThat(count("report_category_limits", 2026)).isGreaterThan(0);
        assertThat(count("report_recurring_merchants", 2026)).isGreaterThan(0);

        var page = store.findTransactions(new TransactionQuery(
                2026,
                0,
                500,
                "postedDate,desc",
                "2026-01",
                null,
                "biedronka",
                "Potrzeby",
                "Koszty codzienne",
                "Potrzeby podstawowe",
                "Żywność i chemia",
                "Market spożywczy"
        ));

        assertThat(page.size()).isEqualTo(200);
        assertThat(page.totalItems()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(row -> {
            assertThat(row.merchant()).isEqualTo("BIEDRONKA");
            assertThat(row.spend()).isEqualByComparingTo(BigDecimal.valueOf(500));
        });
    }

    private com.budget.application.importing.ImportSummary importFixture() throws IOException {
        var bytes = resourceBytes("/fixtures/mbank/fake-2026.csv");
        return importService.importUpload(new TransactionImportFile(
                "lista_operacji_260101_260331_fake.csv",
                bytes.length,
                "text/csv",
                () -> new ByteArrayInputStream(bytes)
        ));
    }

    private byte[] resourceBytes(String resourcePath) throws IOException {
        try (var input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing test resource " + resourcePath);
            }
            return input.readAllBytes();
        }
    }

    private long count(String table, int year) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE report_year = :year",
                Map.of("year", year),
                Long.class
        );
    }
}
