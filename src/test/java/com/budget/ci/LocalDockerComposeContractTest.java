package com.budget.ci;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LocalDockerComposeContractTest {
    @Test
    void composeProvidesLocalPostgresAndOptionalAppWithoutOauthOrFolderRebuild() throws IOException {
        var compose = read("compose.yaml");

        assertThat(compose)
                .contains("image: postgres:17-alpine")
                .contains("${POSTGRES_HOST_BIND:-127.0.0.1}:${POSTGRES_HOST_PORT:-15432}:5432")
                .contains("profiles: [\"app\"]")
                .contains("dockerfile: Dockerfile.local")
                .contains("APP_OAUTH_ENABLED: \"false\"")
                .contains("APP_LOCAL_REBUILD_ENABLED: \"false\"")
                .contains("postgresql://budget:budget@postgres:5432/budget?sslmode=disable")
                .doesNotContain("2025")
                .doesNotContain("2026")
                .doesNotContain("outputs");
    }

    @Test
    void dockerignoreAllowsLocalBuildInputsButKeepsPrivateDataOut() throws IOException {
        var dockerignore = read(".dockerignore");

        assertThat(dockerignore)
                .contains("!Dockerfile.local")
                .contains("!pom.xml")
                .contains("!mvnw")
                .contains("!src/main/java/**")
                .contains("!src/main/resources/**")
                .contains("src/test/")
                .contains("!frontend/package-lock.json")
                .contains("!frontend/src/**")
                .contains("2025/")
                .contains("2026/")
                .contains("20[0-9][0-9]/")
                .contains("*.csv")
                .contains("*.xlsx")
                .contains(".env.*");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
