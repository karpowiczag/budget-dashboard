package com.budget.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
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
        assertThat(input.transactions().getFirst().amount()).isEqualByComparingTo(BigDecimal.valueOf(-123.45));
    }

    @Test
    void stripsUtf8BomFromHeader() throws Exception {
        var csv = """
                \uFEFF#Data operacji;#Rachunek;#Opis operacji;#Kategoria;#Kwota
                2026-01-05;123;LIDL ZAKUP;Bez kategorii;-10,00 PLN
                """;

        var input = reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "lista_operacji_260101_260505.csv", null);

        assertThat(input.transactions()).hasSize(1);
        assertThat(input.transactions().getFirst().description()).isEqualTo("LIDL ZAKUP");
    }

    @Test
    void rejectsCsvWithoutBankHeader() {
        var csv = "date;description;amount\n2026-01-01;test;-1";

        assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "bad.csv", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#Data operacji");
    }
}
