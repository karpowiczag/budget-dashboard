package com.budget.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BankCsvReaderTest {
    private final BankCsvReader reader = new BankCsvReader();

    @Test
    void readsBankCsvAfterMetadataRows() throws Exception {
        var csv = """
                ignored;metadata
                #Data operacji;#Rachunek;#Opis operacji;#Kategoria;#Kwota
                2026-01-05;123;BIEDRONKA ZAKUP;Bez kategorii;-123,45 PLN
                """;

        var input = reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "lista_operacji_260101_260505.csv", null);

        assertThat(input.year()).isEqualTo(2026);
        assertThat(input.transactions()).hasSize(1);
        assertThat(input.transactions().getFirst().amount()).isEqualTo(-123.45);
    }
}
