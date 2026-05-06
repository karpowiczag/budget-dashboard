package com.budget.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.budget.application.importing.BudgetImportService;
import com.budget.application.importing.ImportSummary;
import com.budget.application.importing.TransactionImportFile;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.TransactionQuery;
import com.budget.application.reporting.TransactionRecord;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:fake_csv_import;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
}, webEnvironment = WebEnvironment.RANDOM_PORT)
class FakeCsvImportIntegrationTest {
    @Autowired
    private BudgetImportService importService;

    @Autowired
    private BudgetReportStore store;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Test
    void importsAnalyzesPersistsAndServesFakeMbankExportsThroughV1Api() throws Exception {
        var summary2026 = importFixture("/fixtures/mbank/fake-2026.csv", "lista_operacji_260101_260331_fake.csv");
        var summary2025 = importFixture("/fixtures/mbank/fake-2025.csv", "lista_operacji_250101_251231_fake.csv");

        assertSummary(summary2026, 2026, 17, 70_000, 5_080);
        assertSummary(summary2025, 2025, 6, 29_000, 1_550);

        assertThat(store.findYears()).anySatisfy(row -> {
            assertThat(row.year()).isEqualTo(2025);
            assertThat(row.transactions()).isEqualTo(6);
            assertThat(row.income()).isEqualByComparingTo(BigDecimal.valueOf(29_000));
            assertThat(row.spend()).isEqualByComparingTo(BigDecimal.valueOf(1_550));
        }).anySatisfy(row -> {
            assertThat(row.year()).isEqualTo(2026);
            assertThat(row.transactions()).isEqualTo(17);
            assertThat(row.income()).isEqualByComparingTo(BigDecimal.valueOf(70_000));
            assertThat(row.spend()).isEqualByComparingTo(BigDecimal.valueOf(5_080));
        });

        var dashboard2026 = store.findDashboard(2026);
        assertThat(dashboard2026.year()).isEqualTo(2026);
        assertThat(dashboard2026.activeMonths()).isEqualTo(3);
        assertThat(dashboard2026.kpis().income()).isEqualByComparingTo(BigDecimal.valueOf(70_000));
        assertThat(dashboard2026.kpis().spend()).isEqualByComparingTo(BigDecimal.valueOf(5_080));
        assertThat(dashboard2026.kpis().excludedOutgoing()).isEqualByComparingTo(BigDecimal.valueOf(70_000));
        assertThat(dashboard2026.kpis().excludedIncoming()).isEqualByComparingTo(BigDecimal.valueOf(120));
        assertThat(dashboard2026.kpis().toCheck()).isEqualTo(1);
        assertThat(dashboard2026.kpis().toCheckAmount()).isEqualByComparingTo(BigDecimal.valueOf(750));

        assertMonthly(dashboard2026, "01.2026", 34_000, 3_460, 10);
        assertMonthly(dashboard2026, "02.2026", 18_000, 810, 4);
        assertMonthly(dashboard2026, "03.2026", 18_000, 810, 3);

        assertTransaction("RĘCZNA SPŁATA KARTY KREDYT", "Spłata karty kredytowej", 0, 65_000, 0, "Transfer techniczny");
        assertTransaction("ZWROT ZAKUPU SKLEP TESTOWY", "Zwroty i korekty", 0, 0, 120, "Transfer techniczny");
        assertTransaction("PRZELEW DO BM MBANKU IKZE", "Oszczędności i inwestycje", 0, 3_000, 0, "Oszczędzanie/inwestycje");
        assertTransaction("SKLEP TAJEMNICZY", "Do sprawdzenia", 750, 0, 0, "Do sprawdzenia");
        assertThat(findTransaction("BIEDRONKA ZAKUP; KASA 1").subcategory()).isEqualTo("Market spożywczy");

        assertThat(dashboard2026.recurring())
                .anySatisfy(row -> {
                    assertThat(row.merchant()).isEqualTo("NETFLIX");
                    assertThat(row.category()).isEqualTo("Multimedia, książki i prasa");
                    assertThat(row.months()).isEqualTo(3);
                    assertThat(row.monthlyAverage()).isEqualByComparingTo(BigDecimal.valueOf(60));
                });

        assertThat(store.findDashboard(2025).year()).isEqualTo(2025);
        assertThat(store.findImportRuns())
                .filteredOn(run -> "ok".equals(run.status()))
                .extracting(run -> run.year())
                .contains(2025, 2026);

        var years = getList("/api/v1/years", Map.of());
        assertThat(years).anySatisfy(row -> assertThat(row).containsEntry("year", 2026));

        var settings = getMap("/api/v1/settings/budget", Map.of());
        assertThat(new BigDecimal(settings.get("targetMonthlySpend").toString())).isEqualByComparingTo(BigDecimal.valueOf(14_000));
        var updatedSettings = putJson("/api/v1/settings/budget", """
                {
                  "targetMonthlySpend": 12500,
                  "aggressiveMonthlySpend": 11500,
                  "emergencyFundMinMonths": 4,
                  "emergencyFundComfortMonths": 8,
                  "categoryLimits": [
                    {"category": "Jedzenie poza domem", "limit": 500, "action": "test"}
                  ]
                }
                """);
        assertThat(new BigDecimal(updatedSettings.get("targetMonthlySpend").toString())).isEqualByComparingTo(BigDecimal.valueOf(12_500));
        assertThat(putStatus("/api/v1/settings/budget", "{")).isEqualTo(400);

        var dashboard = getMap("/api/v1/reports/2026/dashboard", Map.of());
        assertThat(dashboard).containsEntry("year", 2026);
        assertThat(dashboard).doesNotContainKey("transactions");

        var transactionPage = getMap("/api/v1/reports/2026/transactions", Map.of("page", "0", "size", "5"));
        assertThat((List<?>) transactionPage.get("items")).hasSize(5);
        assertThat(transactionPage).containsEntry("totalItems", 17).containsEntry("size", 5);

        var filteredPage = getMap("/api/v1/reports/2026/transactions", Map.of("month", "2026-01", "category", "Żywność i chemia"));
        assertThat(filteredPage).containsEntry("totalItems", 2);
        assertThat(items(filteredPage)).extracting(row -> row.get("merchant")).contains("BIEDRONKA", "LIDL");

        var analytics = getMap("/api/v1/reports/2026/analytics", Map.of("scope", "month", "month", "2026-01"));
        assertThat(analytics).containsEntry("scope", "month").containsEntry("transactionCount", 10);
        assertThat((List<?>) analytics.get("categoryTop")).isNotEmpty();

        var calendar = getMap("/api/v1/reports/2026/calendar", Map.of("month", "2026-01"));
        assertThat((List<?>) calendar.get("days")).hasSize(31);

        assertThat(getStatus("/api/v1/reports/2026/calendar", Map.of("month", "bad"))).isEqualTo(400);
        assertThat(getStatus("/api/v1/reports/2026/analytics", Map.of("scope", "month"))).isEqualTo(400);
        assertThat(getStatus("/api/v1/reports/2026/analytics", Map.of("scope", "day", "month", "2026-01"))).isEqualTo(400);
        assertThat(getStatus("/api/v1/reports/2026/transactions", Map.of("month", "bad"))).isEqualTo(400);
        assertThat(getStatus("/api/budget/2026", Map.of())).isEqualTo(404);
    }

    @Test
    void rejectsCsvUploadWithNonCsvContentType() throws Exception {
        var bytes = resourceBytes("/fixtures/mbank/fake-2026.csv");

        assertThatThrownBy(() -> importService.importUpload(new TransactionImportFile(
                "lista_operacji_260101_260331_fake.csv",
                bytes.length,
                "application/json",
                () -> new ByteArrayInputStream(bytes)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only CSV-compatible content types are accepted");

        assertThat(store.findImportRuns())
                .anySatisfy(run -> {
                    assertThat(run.status()).isEqualTo("error");
                    assertThat(run.inputCsv()).isEqualTo("lista_operacji_260101_260331_fake.csv");
                    assertThat(run.message()).contains("CSV-compatible");
                });
    }

    @Test
    void keepsFailedImportAuditWhenAcceptedCsvCannotBeParsed() {
        var bytes = "not-a-bank-export".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> importService.importUpload(new TransactionImportFile(
                "broken.csv",
                bytes.length,
                "text/csv",
                () -> new ByteArrayInputStream(bytes)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Import failed");

        assertThat(store.findImportRuns())
                .anySatisfy(run -> {
                    assertThat(run.status()).isEqualTo("error");
                    assertThat(run.inputCsv()).isEqualTo("broken.csv");
                    assertThat(run.message()).contains("bank header");
                });
    }

    private ImportSummary importFixture(String resourcePath, String fileName) throws IOException {
        var bytes = resourceBytes(resourcePath);
        return importService.importUpload(new TransactionImportFile(fileName, bytes.length, () -> new ByteArrayInputStream(bytes)));
    }

    private void assertSummary(ImportSummary summary, int year, int transactions, double income, double spend) {
        assertThat(summary.status()).isEqualTo("ok");
        assertThat(summary.years()).containsExactly(year);
        assertThat(summary.transactions()).isEqualTo(transactions);
        assertThat(summary.income()).isEqualByComparingTo(BigDecimal.valueOf(income));
        assertThat(summary.spend()).isEqualByComparingTo(BigDecimal.valueOf(spend));
        assertThat(summary.message()).isEqualTo("CSV imported; raw file was not retained");
    }

    private byte[] resourceBytes(String resourcePath) throws IOException {
        try (var input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing test resource " + resourcePath);
            }
            return input.readAllBytes();
        }
    }

    private void assertMonthly(com.budget.domain.report.BudgetSnapshot dashboard, String month, double income, double spend, int transactions) {
        var row = dashboard.monthly().stream()
                .filter(item -> month.equals(item.month()))
                .findFirst()
                .orElseThrow();
        assertThat(row.income()).isEqualByComparingTo(BigDecimal.valueOf(income));
        assertThat(row.spend()).isEqualByComparingTo(BigDecimal.valueOf(spend));
        assertThat(row.transactions()).isEqualTo(transactions);
    }

    private void assertTransaction(
            String description,
            String category,
            double spend,
            double excludedOutgoing,
            double excludedIncoming,
            String bucket
    ) {
        var row = findTransaction(description);
        assertThat(row.correctedCategory()).isEqualTo(category);
        assertThat(row.bucket()).isEqualTo(bucket);
        assertThat(row.spend()).isEqualByComparingTo(BigDecimal.valueOf(spend));
        assertThat(row.excludedOutgoing()).isEqualByComparingTo(BigDecimal.valueOf(excludedOutgoing));
        assertThat(row.excludedIncoming()).isEqualByComparingTo(BigDecimal.valueOf(excludedIncoming));
    }

    private TransactionRecord findTransaction(String description) {
        var page = store.findTransactions(new TransactionQuery(2026, 0, 50, "postedDate,desc", null, null, description, null, null, null, null, null));
        return page.items().stream()
                .filter(row -> description.equals(row.description()))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(String path, Map<String, String> params) throws Exception {
        var response = get(path, params);
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readValue(response.body(), new TypeReference<>() {
        });
    }

    private List<Map<String, Object>> getList(String path, Map<String, String> params) throws Exception {
        var response = get(path, params);
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readValue(response.body(), new TypeReference<>() {
        });
    }

    private int getStatus(String path, Map<String, String> params) throws Exception {
        return get(path, params).statusCode();
    }

    private HttpResponse<String> get(String path, Map<String, String> params) throws Exception {
        var uri = URI.create("http://localhost:" + port + path + queryString(params));
        var request = HttpRequest.newBuilder(uri).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> putJson(String path, String body) throws Exception {
        var uri = URI.create("http://localhost:" + port + path);
        var request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readValue(response.body(), new TypeReference<>() {
        });
    }

    private int putStatus(String path, String body) throws Exception {
        var uri = URI.create("http://localhost:" + port + path);
        var request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private String queryString(Map<String, String> params) {
        if (params.isEmpty()) {
            return "";
        }
        var query = new StringBuilder("?");
        params.forEach((key, value) -> {
            if (query.length() > 1) {
                query.append("&");
            }
            query.append(encode(key)).append("=").append(encode(value));
        });
        return query.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("items");
    }
}
