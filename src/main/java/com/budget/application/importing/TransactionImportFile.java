package com.budget.application.importing;

import java.io.IOException;
import java.io.InputStream;

public record TransactionImportFile(String originalFileName, long size, String contentType, Content content) {
    public TransactionImportFile(String originalFileName, long size, Content content) {
        this(originalFileName, size, "", content);
    }

    public TransactionImportFile {
        contentType = contentType == null ? "" : contentType;
        if (content == null) {
            throw new IllegalArgumentException("Import file content is required");
        }
    }

    InputStream openStream() throws IOException {
        return content.openStream();
    }

    @FunctionalInterface
    public interface Content {
        InputStream openStream() throws IOException;
    }
}
