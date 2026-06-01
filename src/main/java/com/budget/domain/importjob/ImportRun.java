package com.budget.domain.importjob;

import java.time.OffsetDateTime;

public record ImportRun(
        long id,
        Integer year,
        String inputCsv,
        String status,
        String message,
        int duplicatesRemoved,
        OffsetDateTime createdAt
) {
}
