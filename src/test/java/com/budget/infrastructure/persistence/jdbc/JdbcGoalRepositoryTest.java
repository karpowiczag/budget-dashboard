package com.budget.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.goal.GoalService;
import com.budget.domain.goal.Goal;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:budget_goals;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class JdbcGoalRepositoryTest {
    @Autowired
    private GoalService service;

    @Test
    void savesUpsertsOrdersAndDeletesGoals() {
        var afterSave = service.save(new Goal("house", "Wkład na mieszkanie",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), LocalDate.of(2028, 1, 1), "20% wkładu"));
        assertThat(afterSave).hasSize(1);
        assertThat(afterSave.get(0).targetAmount()).isEqualByComparingTo("100000.00");
        assertThat(afterSave.get(0).currentAmount()).isEqualByComparingTo("20000.00");

        // Upsert by id.
        var afterUpdate = service.save(new Goal("house", "Wkład na mieszkanie",
                new BigDecimal("100000.00"), new BigDecimal("35000.00"), LocalDate.of(2028, 1, 1), "update"));
        assertThat(afterUpdate).hasSize(1);
        assertThat(afterUpdate.get(0).currentAmount()).isEqualByComparingTo("35000.00");

        // Earlier target date sorts first.
        service.save(new Goal("car", "Auto", new BigDecimal("40000.00"), new BigDecimal("0.00"), LocalDate.of(2027, 1, 1), null));
        assertThat(service.list()).extracting(Goal::goalId).containsExactly("car", "house");

        // Null currentAmount is coerced to zero.
        service.save(new Goal("emergency", "Poduszka", new BigDecimal("30000.00"), null, null, null));
        assertThat(service.list()).filteredOn(goal -> goal.goalId().equals("emergency")).singleElement()
                .satisfies(goal -> assertThat(goal.currentAmount()).isEqualByComparingTo("0.00"));

        service.delete("car");
        assertThat(service.list()).extracting(Goal::goalId).doesNotContain("car");
    }
}
