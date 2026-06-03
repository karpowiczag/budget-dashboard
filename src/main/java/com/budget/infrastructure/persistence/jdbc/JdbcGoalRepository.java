package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.goal.GoalStore;
import com.budget.domain.goal.Goal;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcGoalRepository implements GoalStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcGoalRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Goal> goals() {
        return jdbc.query("SELECT * FROM financial_goals ORDER BY target_date NULLS LAST, name", new MapSqlParameterSource(), this::map);
    }

    @Override
    @Transactional
    public Goal save(Goal goal) {
        jdbc.update("DELETE FROM financial_goals WHERE goal_id = :goalId",
                new MapSqlParameterSource("goalId", goal.goalId()));
        jdbc.update("""
                INSERT INTO financial_goals (
                    goal_id, name, target_amount, current_amount, target_date, note, updated_at
                ) VALUES (
                    :goalId, :name, :targetAmount, :currentAmount, :targetDate, :note, :updatedAt
                )
                """, new MapSqlParameterSource()
                .addValue("goalId", goal.goalId())
                .addValue("name", goal.name())
                .addValue("targetAmount", goal.targetAmount())
                .addValue("currentAmount", goal.currentAmount() == null ? BigDecimal.ZERO : goal.currentAmount())
                .addValue("targetDate", goal.targetDate())
                .addValue("note", goal.note())
                .addValue("updatedAt", OffsetDateTime.now()));
        return goal;
    }

    @Override
    @Transactional
    public void delete(String goalId) {
        jdbc.update("DELETE FROM financial_goals WHERE goal_id = :goalId",
                new MapSqlParameterSource("goalId", goalId));
    }

    private Goal map(ResultSet rs, int rowNum) throws SQLException {
        return new Goal(
                rs.getString("goal_id"),
                rs.getString("name"),
                rs.getBigDecimal("target_amount"),
                rs.getBigDecimal("current_amount"),
                rs.getObject("target_date", LocalDate.class),
                rs.getString("note")
        );
    }
}
