package com.budget.infrastructure;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

public interface ImportRunDataJdbcRepository extends ListCrudRepository<ImportRunEntity, Long> {
    List<ImportRunEntity> findTop50ByOrderByIdDesc();
}
