package com.budget.ci;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryWorkflowContractTest {
    private static final List<String> REQUIRED_DEPLOY_SECRETS = List.of(
            "RENDER_API_KEY",
            "RENDER_SERVICE_ID",
            "RENDER_PUBLIC_URL"
    );

    @Test
    void normalCiStaysLightweightAndDoesNotRunNativeCompilation() throws IOException {
        var ci = read(".github/workflows/ci.yml");

        assertThat(ci)
                .contains("pull_request:")
                .contains("branches: [main, develop]")
                .contains("npm test")
                .contains("npm run build")
                .contains("npm run sync:backend")
                .contains("./mvnw test")
                .doesNotContain("native:compile");
    }

    @Test
    void deployWorkflowIsManualMainOnlyAndWaitsForRenderDeploy() throws IOException {
        var deploy = read(".github/workflows/deploy.yml");

        assertThat(deploy)
                .contains("workflow_dispatch:")
                .contains("if: github.ref == 'refs/heads/main'")
                .contains("./mvnw -Pnative -DskipTests native:compile")
                .contains("Validate deployment secrets")
                .contains("Install Render CLI")
                .contains("Deploy image to Render")
                .contains("render deploys create \"$RENDER_SERVICE_ID\" --image \"$IMAGE\" --wait")
                .contains("curl --fail --silent --show-error --retry 30");

        for (var secret : REQUIRED_DEPLOY_SECRETS) {
            assertThat(deploy)
                    .as(secret + " is validated from GitHub secrets")
                    .contains(secret + ": ${{ secrets." + secret + " }}")
                    .contains(secret);
        }
        assertThat(deploy).contains("::error::$name is required for a release deployment");
        assertThat(deploy.toLowerCase()).doesNotContain("koy" + "eb");
    }

    @Test
    void uiSmokeIsManualIsolatedAndAuthDisabled() throws IOException {
        var smoke = read(".github/workflows/ui-smoke.yml");

        assertThat(smoke)
                .contains("workflow_dispatch:")
                .contains("DATABASE_URL: jdbc:h2:mem:ui_smoke")
                .contains("APP_OAUTH_ENABLED: false")
                .contains("npm run smoke:ui")
                .contains("npx playwright install --with-deps chromium")
                .doesNotContain("pull_request:")
                .doesNotContain("native:compile");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
