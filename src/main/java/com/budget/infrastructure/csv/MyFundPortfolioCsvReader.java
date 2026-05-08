package com.budget.infrastructure.csv;

import com.budget.application.fire.FirePortfolioReader;
import com.budget.domain.fire.FirePortfolioPosition;
import com.budget.domain.fire.FirePortfolioSnapshot;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MyFundPortfolioCsvReader implements FirePortfolioReader {
    private static final Charset WINDOWS_1250 = Charset.forName("windows-1250");
    private static final Pattern SNAPSHOT_DATE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    @Override
    public FirePortfolioSnapshot read(Path reportsPath) throws IOException {
        if (reportsPath == null || !Files.isDirectory(reportsPath)) {
            return new FirePortfolioSnapshot(null, List.of(), List.of());
        }
        var positions = new ArrayList<FirePortfolioPosition>();
        var sources = new ArrayList<String>();
        try (var files = Files.list(reportsPath)) {
            var csvFiles = files
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            for (var file : csvFiles) {
                sources.add(file.getFileName().toString());
                positions.addAll(readFile(file));
            }
        }
        var asOf = positions.stream()
                .map(FirePortfolioPosition::priceDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        return new FirePortfolioSnapshot(asOf, positions, sources);
    }

    private List<FirePortfolioPosition> readFile(Path file) throws IOException {
        var fileName = file.getFileName().toString();
        var text = readText(file);
        var rows = new ArrayList<FirePortfolioPosition>();
        try (var reader = new BufferedReader(new StringReader(text))) {
            var headerLine = reader.readLine();
            if (headerLine == null || !headerLine.startsWith("Walor")) {
                return List.of();
            }
            var headers = parseLine(stripBom(headerLine));
            var line = "";
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                var values = parseLine(line);
                while (values.size() < headers.size()) {
                    values.add("");
                }
                var row = new LinkedHashMap<String, String>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i), values.get(i));
                }
                var instrument = clean(row.get("Walor"));
                var group = clean(row.get("Grupa"));
                if (instrument.isBlank() || instrument.equalsIgnoreCase("Razem") || group.equalsIgnoreCase("Razem")) {
                    continue;
                }
                var value = amount(row.get("Wartość waloru [PLN]"));
                if (value.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                rows.add(new FirePortfolioPosition(
                        fileName,
                        portfolioName(fileName),
                        wrapper(fileName, row),
                        assetClass(group, instrument, row),
                        instrument,
                        group,
                        clean(row.get("Konto")),
                        clean(row.get("Waluta waloru")),
                        clean(row.get("ISIN")),
                        date(row.get("Data ceny aktualnej ***"), fileName),
                        amount(row.get("Liczba jednostek")),
                        amount(row.get("Wartość zakupu [PLN]")),
                        value,
                        amount(row.get("Zysk [PLN]")),
                        percent(row.get("Stopa zwrotu [%]"))
                ));
            }
        }
        return rows;
    }

    private String readText(Path file) throws IOException {
        var bytes = Files.readAllBytes(file);
        var utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (utf8.indexOf('\uFFFD') < 0) {
            return utf8;
        }
        return new String(bytes, WINDOWS_1250);
    }

    private String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private List<String> parseLine(String line) {
        var values = new ArrayList<String>();
        var current = new StringBuilder();
        var quoted = false;
        for (var i = 0; i < line.length(); i++) {
            var ch = line.charAt(i);
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

    private String portfolioName(String fileName) {
        var name = fileName
                .replaceFirst("^myfund\\.pl_", "")
                .replaceFirst("_portfelSklad_\\d{4}-\\d{2}-\\d{2}\\.csv$", "")
                .replace('_', ' ')
                .trim();
        if (name.equalsIgnoreCase("Gielda")) {
            return "Giełda";
        }
        if (name.equalsIgnoreCase("poduszka")) {
            return "Poduszka";
        }
        return name;
    }

    private String wrapper(String fileName, LinkedHashMap<String, String> row) {
        var combined = (fileName + " " + row.getOrDefault("Konto", "") + " " + row.getOrDefault("Tagi", "")).toLowerCase(Locale.ROOT);
        if (combined.contains("poduszka") || combined.contains("oszczęd") || combined.contains("oszczed") || combined.contains("gotów")) {
            return "Poduszka bezpieczeństwa";
        }
        if (combined.contains("emeryt") || combined.contains("ike") || combined.contains("ikze") || combined.contains("ppk")) {
            return "Emerytalne długoterminowe";
        }
        return "Rachunek opodatkowany";
    }

    private String assetClass(String group, String instrument, LinkedHashMap<String, String> row) {
        var text = (group + " " + instrument + " " + row.getOrDefault("Tagi", "")).toLowerCase(Locale.ROOT);
        if (text.contains("konto") || text.contains("gotów") || text.contains("cash") || text.contains("lokata") || text.contains("pienięż")) {
            return "Gotówka";
        }
        if (text.contains("oblig") || text.contains("bond")) {
            return "Obligacje";
        }
        if (text.contains("etc") || text.contains("gold") || text.contains("złoto") || text.contains("surow")) {
            return "Alternatywne";
        }
        if (text.contains("akcje") || text.contains("etf") || text.contains("fundusz") || text.contains("fundusze")) {
            return "Akcje";
        }
        return "Inne";
    }

    private LocalDate date(String raw, String fileName) {
        var cleaned = clean(raw);
        if (cleaned.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(cleaned);
        }
        if (cleaned.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
            var parts = cleaned.split("\\.");
            return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
        }
        var matcher = SNAPSHOT_DATE.matcher(fileName);
        return matcher.find() ? LocalDate.parse(matcher.group(1)) : null;
    }

    private BigDecimal percent(String raw) {
        return amount(raw).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(String raw) {
        var cleaned = clean(raw)
                .replace("\u00a0", "")
                .replace(" ", "")
                .replace("%", "")
                .replace("PLN", "")
                .replace(",", ".")
                .replaceAll("[^0-9.\\-]", "");
        var lastDot = cleaned.lastIndexOf('.');
        if (lastDot >= 0) {
            cleaned = cleaned.substring(0, lastDot).replace(".", "") + cleaned.substring(lastDot);
        }
        if (cleaned.isBlank() || cleaned.equals("-") || cleaned.equals(".")) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
