package com.budget.domain;

import java.time.OffsetDateTime;

public record ImportRun(
        long id,
        Integer year,
        String inputCsv,
        String status,
        String message,
        OffsetDateTime createdAt
) {
}
