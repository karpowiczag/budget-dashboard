package com.budget.infrastructure;

import org.springframework.data.repository.ListCrudRepository;

public interface ReportDataJdbcRepository extends ListCrudRepository<ReportEntity, Integer> {
}
