package com.budget.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.web.controller.BudgetApiController;
import com.budget.web.controller.CategoryApiController;
import com.budget.web.controller.FireApiController;
import com.budget.web.controller.GoalApiController;
import com.budget.web.controller.NetWorthApiController;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:openapi_contract;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class OpenApiContractTest {
    private static final Map<String, String> OPERATION_IDS = Map.ofEntries(
            Map.entry("session", "getSession"),
            Map.entry("years", "listYears"),
            Map.entry("dashboard", "getReportDashboard"),
            Map.entry("calendar", "getReportCalendar"),
            Map.entry("analytics", "getReportAnalytics"),
            Map.entry("transactions", "listReportTransactions"),
            Map.entry("recategorizeTransaction", "recategorizeTransaction"),
            Map.entry("upload", "uploadImportCsv"),
            Map.entry("rebuild", "rebuildImports"),
            Map.entry("importRuns", "listImportRuns"),
            Map.entry("budgetSettings", "getBudgetSettings"),
            Map.entry("saveBudgetSettings", "updateBudgetSettings"),
            Map.entry("summary", "getFireSummary"),
            Map.entry("settings", "getFireSettings"),
            Map.entry("saveSettings", "updateFireSettings"),
            Map.entry("netWorth", "getNetWorth"),
            Map.entry("saveAccount", "updateNetWorthAccount"),
            Map.entry("saveLiability", "updateNetWorthLiability"),
            Map.entry("deleteLiability", "deleteNetWorthLiability"),
            Map.entry("listGoals", "listGoals"),
            Map.entry("updateGoal", "updateGoal"),
            Map.entry("deleteGoal", "deleteGoal"),
            Map.entry("listCategories", "listCategories"),
            Map.entry("updateCategory", "updateCategory"),
            Map.entry("updateCategoryGroup", "updateCategoryGroup"),
            Map.entry("deleteCategory", "deleteCategory"),
            Map.entry("reorderCategories", "reorderCategories"),
            Map.entry("updateCategoryRule", "updateCategoryRule"),
            Map.entry("deleteCategoryRule", "deleteCategoryRule")
    );
    private static final Map<String, String> SUCCESS_SCHEMAS = Map.ofEntries(
            Map.entry("getSession", "SessionResponse"),
            Map.entry("listYears", "[YearSummary]"),
            Map.entry("getReportDashboard", "DashboardResponse"),
            Map.entry("getReportCalendar", "CalendarResponse"),
            Map.entry("getReportAnalytics", "AnalyticsResponse"),
            Map.entry("listReportTransactions", "TransactionPage"),
            Map.entry("recategorizeTransaction", "RecategorizeResponse"),
            Map.entry("uploadImportCsv", "ImportSummary"),
            Map.entry("rebuildImports", "ImportSummary"),
            Map.entry("listImportRuns", "[ImportRun]"),
            Map.entry("getBudgetSettings", "BudgetSettings"),
            Map.entry("updateBudgetSettings", "BudgetSettings"),
            Map.entry("getFireSummary", "FireSummary"),
            Map.entry("getFireSettings", "FireSettings"),
            Map.entry("updateFireSettings", "FireSettings"),
            Map.entry("getNetWorth", "NetWorthResponse"),
            Map.entry("updateNetWorthAccount", "NetWorthResponse"),
            Map.entry("updateNetWorthLiability", "NetWorthResponse"),
            Map.entry("deleteNetWorthLiability", "NetWorthResponse"),
            Map.entry("listGoals", "[Goal]"),
            Map.entry("updateGoal", "[Goal]"),
            Map.entry("deleteGoal", "[Goal]"),
            Map.entry("listCategories", "CategoriesResponse"),
            Map.entry("updateCategory", "CategoriesResponse"),
            Map.entry("updateCategoryGroup", "CategoriesResponse"),
            Map.entry("deleteCategory", "CategoriesResponse"),
            Map.entry("reorderCategories", "CategoriesResponse"),
            Map.entry("updateCategoryRule", "CategoriesResponse"),
            Map.entry("deleteCategoryRule", "CategoriesResponse")
    );

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void openApiSpecParsesAndMatchesBudgetControllerMappings() {
        var result = new OpenAPIV3Parser().readLocation(Path.of("docs/openapi/budget-api.yaml").toUri().toString(), null, null);

        assertThat(result.getMessages()).isEmpty();
        var openApi = result.getOpenAPI();
        assertThat(openApi).isNotNull();

        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (!BudgetApiController.class.equals(handler.getBeanType())
                    && !FireApiController.class.equals(handler.getBeanType())
                    && !NetWorthApiController.class.equals(handler.getBeanType())
                    && !GoalApiController.class.equals(handler.getBeanType())
                    && !CategoryApiController.class.equals(handler.getBeanType())) {
                return;
            }
            assertWebDtoReturnType(handler.getMethod().getGenericReturnType());
            var expectedOperationId = OPERATION_IDS.get(handler.getMethod().getName());
            assertThat(expectedOperationId).as("operation id for " + handler.getMethod()).isNotBlank();
            mapping.getPatternValues().forEach(path -> {
                assertThat(openApi.getPaths()).containsKey(path);
                mapping.getMethodsCondition().getMethods().forEach(method -> {
                    var operation = operation(openApi.getPaths().get(path), method);
                    assertThat(operation).as(path + " " + method).isNotNull();
                    assertThat(operation.getOperationId()).isEqualTo(expectedOperationId);
                    assertSuccessSchema(operation, SUCCESS_SCHEMAS.get(expectedOperationId));
                });
            });
        });

        assertRef(openApi.getComponents().getSchemas().get("DashboardResponse"), "monthControl", "MonthControl");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("DashboardResponse"), "fixedness", "FixednessSummary");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("DashboardResponse"), "topMerchants", "MerchantSummary");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("DashboardResponse"), "recurring", "RecurringItem");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("DashboardResponse"), "largeOneoffs", "LargeOneOff");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "areaTop", "AreaSpend");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "hierarchyTop", "HierarchySpend");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "financialFlows", "FinancialFlow");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "monthlyCategoryTrends", "MonthlyCategoryTrend");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "monthlyHierarchyTrends", "MonthlyHierarchyTrend");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "monthlyBucketTrends", "MonthlyBucketTrend");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "monthlyMerchantTrends", "MonthlyMerchantTrend");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "fixednessBreakdown", "FixednessBreakdown");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "confidenceBreakdown", "ConfidenceBreakdown");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("AnalyticsResponse"), "amountBands", "AmountBand");
        assertArrayItemRef(openApi.getComponents().getSchemas().get("FireSummary"), "positionAnalyses", "FirePositionAnalysis");
        assertThat(openApi.getComponents().getSchemas()).containsKeys("BudgetSettings", "CategoryLimitSetting", "FireSettings");
    }

    private io.swagger.v3.oas.models.Operation operation(PathItem pathItem, RequestMethod method) {
        return switch (method) {
            case GET -> pathItem.getGet();
            case POST -> pathItem.getPost();
            case PUT -> pathItem.getPut();
            case PATCH -> pathItem.getPatch();
            case DELETE -> pathItem.getDelete();
            default -> null;
        };
    }

    private void assertWebDtoReturnType(java.lang.reflect.Type returnType) {
        if (returnType instanceof ParameterizedType parameterizedType) {
            assertThat(parameterizedType.getRawType()).isEqualTo(java.util.List.class);
            assertWebDtoReturnType(parameterizedType.getActualTypeArguments()[0]);
            return;
        }
        if (returnType instanceof Class<?> type) {
            assertThat(type.getPackageName()).startsWith("com.budget.web.dto");
        }
    }

    private void assertRef(Schema<?> schema, String property, String target) {
        var propertySchema = (Schema<?>) schema.getProperties().get(property);
        assertThat(propertySchema.get$ref()).isEqualTo("#/components/schemas/" + target);
    }

    private void assertArrayItemRef(Schema<?> schema, String property, String target) {
        var propertySchema = (Schema<?>) schema.getProperties().get(property);
        assertThat(propertySchema.getItems().get$ref()).isEqualTo("#/components/schemas/" + target);
    }

    private void assertSuccessSchema(io.swagger.v3.oas.models.Operation operation, String expectedSchema) {
        assertThat(expectedSchema).as("success schema for " + operation.getOperationId()).isNotBlank();
        var response = operation.getResponses().get("200");
        assertThat(response).as("200 response for " + operation.getOperationId()).isNotNull();
        var schema = response.getContent().get("application/json").getSchema();
        if (expectedSchema.startsWith("[")) {
            var itemSchema = expectedSchema.substring(1, expectedSchema.length() - 1);
            assertThat(schema.getItems()).as("array items for " + operation.getOperationId()).isNotNull();
            assertThat(schema.getItems().get$ref()).isEqualTo("#/components/schemas/" + itemSchema);
            return;
        }
        assertThat(schema.get$ref()).isEqualTo("#/components/schemas/" + expectedSchema);
    }
}
