package com.budget.infrastructure;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface BudgetTransactionDataJdbcRepository extends ListCrudRepository<BudgetTransactionEntity, Long> {
    @Modifying
    @Query("DELETE FROM budget_transactions WHERE report_year = :reportYear")
    void deleteByReportYear(@Param("reportYear") int reportYear);
}
