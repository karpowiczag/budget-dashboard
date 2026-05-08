package com.budget.application.fire;

import com.budget.domain.fire.FirePortfolioSnapshot;
import java.io.IOException;
import java.nio.file.Path;

public interface FirePortfolioReader {
    FirePortfolioSnapshot read(Path reportsPath) throws IOException;
}
