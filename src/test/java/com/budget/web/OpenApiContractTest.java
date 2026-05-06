package com.budget.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.web.controller.BudgetApiController;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
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
    private static final Map<String, String> OPERATION_IDS = Map.of(
            "session", "getSession",
            "years", "listYears",
            "dashboard", "getReportDashboard",
            "calendar", "getReportCalendar",
            "analytics", "getReportAnalytics",
            "transactions", "listReportTransactions",
            "upload", "uploadImportCsv",
            "rebuild", "rebuildImports",
            "importRuns", "listImportRuns"
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
            if (!BudgetApiController.class.equals(handler.getBeanType())) {
                return;
            }
            var expectedOperationId = OPERATION_IDS.get(handler.getMethod().getName());
            assertThat(expectedOperationId).as("operation id for " + handler.getMethod()).isNotBlank();
            mapping.getPatternValues().forEach(path -> {
                assertThat(openApi.getPaths()).containsKey(path);
                mapping.getMethodsCondition().getMethods().forEach(method -> {
                    var operation = operation(openApi.getPaths().get(path), method);
                    assertThat(operation).as(path + " " + method).isNotNull();
                    assertThat(operation.getOperationId()).isEqualTo(expectedOperationId);
                });
            });
        });
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
}
