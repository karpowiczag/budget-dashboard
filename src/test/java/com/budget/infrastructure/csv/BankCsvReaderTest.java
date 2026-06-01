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
                ﻿#Data operacji;#Rachunek;#Opis operacji;#Kategoria;#Kwota
                2026-01-05;123;LIDL ZAKUP;Bez kategorii;-10,00 PLN
                """;

        var input = reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "lista_operacji_260101_260505.csv", null);

        assertThat(input.transactions()).hasSize(1);
        assertThat(input.transactions().getFirst().description()).isEqualTo("LIDL ZAKUP");
    }

    @Test
    void parsesAmountsGroupedWithNonBreakingSpaces() throws Exception {
        // Real Polish bank exports group thousands with a non-breaking space (U+00A0) or
        // narrow NBSP (U+202F); a plain ASCII-space strip leaves them and BigDecimal rejects them.
        var csv = "#Data operacji;#Rachunek;#Opis operacji;#Kategoria;#Kwota\n"
                + "2026-01-10;123;WYNAGRODZENIE;Bez kategorii;18 000,00 PLN\n"
                + "2026-01-11;123;CZYNSZ;Bez kategorii;-2 500,50 PLN\n";

        var input = reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "lista_operacji_260101_260131.csv", null);

        assertThat(input.transactions()).hasSize(2);
        assertThat(input.transactions().get(0).amount()).isEqualByComparingTo(new BigDecimal("18000.00"));
        assertThat(input.transactions().get(1).amount()).isEqualByComparingTo(new BigDecimal("-2500.50"));
    }

    @Test
    void rejectsCsvWithoutBankHeader() {
        var csv = "date;description;amount\n2026-01-01;test;-1";

        assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "bad.csv", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#Data operacji");
    }
}
