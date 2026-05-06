package com.budget.application.importing;

import java.nio.file.Path;

public record ImportSettings(long maxUploadBytes, Path localRoot) {
    public ImportSettings {
        if (maxUploadBytes <= 0) {
            throw new IllegalArgumentException("maxUploadBytes must be positive");
        }
        if (localRoot == null) {
            localRoot = Path.of(".");
        }
    }
}
