package com.budget.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MyFundPortfolioCsvReaderTest {
    private final MyFundPortfolioCsvReader reader = new MyFundPortfolioCsvReader();

    @TempDir
    Path tempDir;

    @Test
    void readsMyFundPortfolioCompositionAndSkipsTotalRows() throws Exception {
        var source = Path.of("src/test/resources/fixtures/myfund/fake-portfelSklad.csv");
        var target = tempDir.resolve("myfund.pl_Emerytura_Test_portfelSklad_2026-05-08.csv");
        Files.copy(source, target);

        var snapshot = reader.read(tempDir);

        assertThat(snapshot.sourceFiles()).containsExactly(target.getFileName().toString());
        assertThat(snapshot.positions()).hasSize(3);
        assertThat(snapshot.asOf()).hasToString("2026-05-08");
        assertThat(snapshot.positions())
                .extracting("assetClass")
                .containsExactly("Akcje", "Obligacje", "Gotówka");
        assertThat(snapshot.positions())
                .extracting("wrapper")
                .contains("Emerytalne długoterminowe", "Poduszka bezpieczeństwa");
        assertThat(snapshot.positions().stream()
                .map(position -> position.valuePln())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("2750.00");
    }

    @Test
    void missingReportsDirectoryReturnsEmptySnapshot() throws Exception {
        var snapshot = reader.read(tempDir.resolve("missing"));

        assertThat(snapshot.positions()).isEmpty();
        assertThat(snapshot.sourceFiles()).isEmpty();
        assertThat(snapshot.asOf()).isNull();
    }
}
