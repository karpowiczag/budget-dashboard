package com.budget.domain.fire;

import java.time.LocalDate;
import java.util.List;

public record FirePortfolioSnapshot(
        LocalDate asOf,
        List<FirePortfolioPosition> positions,
        List<String> sourceFiles
) {
    public FirePortfolioSnapshot {
        positions = positions == null ? List.of() : List.copyOf(positions);
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
    }
}
