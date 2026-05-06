package com.budget.infrastructure;

import com.budget.domain.BankTransaction;
import com.budget.domain.BudgetInput;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class BankCsvReader {
    private static final Pattern YEAR_IN_NAME = Pattern.compile("_(\\d{2})0101_");

    public BudgetInput read(InputStream inputStream, String fileName, Integer requestedYear) throws IOException {
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            lines = reader.lines().toList();
        }
        if (!lines.isEmpty() && lines.getFirst().startsWith("\uFEFF")) {
            lines = new ArrayList<>(lines);
            lines.set(0, lines.getFirst().substring(1));
        }
        int headerIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("#Data operacji")) {
                headerIndex = i;
                break;
            }
        }
        if (headerIndex < 0) {
            throw new IllegalArgumentException("CSV does not contain bank header '#Data operacji'");
        }

        List<String> headers = parseLine(lines.get(headerIndex));
        List<BankTransaction> transactions = new ArrayList<>();
        for (int i = headerIndex + 1; i < lines.size(); i++) {
            List<String> values = parseLine(lines.get(i));
            if (values.size() < headers.size()) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                row.put(headers.get(c), values.get(c));
            }
            String rawDate = row.getOrDefault("#Data operacji", "");
            if (!rawDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                continue;
            }
            transactions.add(new BankTransaction(
                    LocalDate.parse(rawDate),
                    clean(row.get("#Rachunek")),
                    clean(row.get("#Opis operacji")),
                    clean(row.get("#Kategoria")),
                    amount(row.get("#Kwota"))
            ));
        }
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("CSV contains no valid transactions");
        }
        int year = requestedYear != null ? requestedYear : inferYear(fileName, transactions);
        return new BudgetInput(year, fileName, transactions);
    }

    private int inferYear(String fileName, List<BankTransaction> transactions) {
        Matcher matcher = YEAR_IN_NAME.matcher(fileName == null ? "" : fileName);
        if (matcher.find()) {
            return 2000 + Integer.parseInt(matcher.group(1));
        }
        return transactions.getFirst().date().getYear();
    }

    private List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ';' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private double amount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String normalized = raw.replace("PLN", "").replace(" ", "").replace(",", ".").trim();
        return round2(Double.parseDouble(normalized));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
