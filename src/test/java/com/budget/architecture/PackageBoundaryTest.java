package com.budget.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PackageBoundaryTest {
    private static final Path MAIN_JAVA = Path.of("src/main/java");

    @Test
    void domainDoesNotDependOnOuterLayers() throws IOException {
        assertNoForbiddenImports(
                MAIN_JAVA.resolve("com/budget/domain"),
                Set.of("com.budget.application", "com.budget.infrastructure", "com.budget.web", "com.budget.config")
        );
    }

    @Test
    void applicationDoesNotDependOnAdaptersOrSpringConfig() throws IOException {
        assertNoForbiddenImports(
                MAIN_JAVA.resolve("com/budget/application"),
                Set.of("com.budget.infrastructure", "com.budget.web", "com.budget.config")
        );
    }

    @Test
    void packageDeclarationsMatchFolderLayout() throws IOException {
        var mismatches = javaFiles(MAIN_JAVA).stream()
                .flatMap(path -> packageMismatch(path).stream())
                .toList();

        assertThat(mismatches).isEmpty();
    }

    private void assertNoForbiddenImports(Path root, Set<String> forbiddenPrefixes) throws IOException {
        var violations = javaFiles(root).stream()
                .flatMap(path -> forbiddenImports(path, forbiddenPrefixes).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    private List<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var files = Files.walk(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private List<String> forbiddenImports(Path path, Set<String> forbiddenPrefixes) {
        try {
            var lines = Files.readAllLines(path);
            return java.util.stream.IntStream.range(0, lines.size())
                    .mapToObj(index -> violation(path, index + 1, lines.get(index), forbiddenPrefixes))
                    .flatMap(java.util.Optional::stream)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }

    private java.util.Optional<String> violation(Path path, int lineNumber, String line, Set<String> forbiddenPrefixes) {
        var trimmed = line.trim();
        if (!trimmed.startsWith("import ")) {
            return java.util.Optional.empty();
        }
        return forbiddenPrefixes.stream()
                .filter(prefix -> trimmed.startsWith("import " + prefix + "."))
                .findFirst()
                .map(prefix -> path + ":" + lineNumber + " imports forbidden package " + prefix);
    }

    private java.util.Optional<String> packageMismatch(Path path) {
        try {
            var expected = expectedPackage(path);
            var actual = Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("package "))
                    .findFirst()
                    .map(line -> line.substring("package ".length(), line.length() - 1))
                    .orElse("");
            if (expected.equals(actual)) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(path + " declares " + actual + " but should declare " + expected);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }

    private String expectedPackage(Path path) {
        var relativeParent = MAIN_JAVA.relativize(path.getParent());
        return relativeParent.toString().replace('\\', '.').replace('/', '.');
    }
}
