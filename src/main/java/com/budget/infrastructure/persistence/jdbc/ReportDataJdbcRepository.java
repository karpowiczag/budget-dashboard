package com.budget.infrastructure.persistence.jdbc;

import org.springframework.data.repository.ListCrudRepository;

public interface ReportDataJdbcRepository extends ListCrudRepository<ReportEntity, Integer> {
}
