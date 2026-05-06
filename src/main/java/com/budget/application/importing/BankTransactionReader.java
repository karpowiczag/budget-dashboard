package com.budget.application.importing;

import com.budget.domain.report.BudgetInput;
import java.io.IOException;
import java.io.InputStream;

public interface BankTransactionReader {
    BudgetInput read(InputStream inputStream, String fileName, Integer requestedYear) throws IOException;
}
