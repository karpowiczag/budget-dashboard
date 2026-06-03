package com.budget.application.goal;

import com.budget.domain.goal.Goal;
import java.util.List;

/** Port for persisting financial goals. Implemented by an adapter in infrastructure. */
public interface GoalStore {
    List<Goal> goals();

    Goal save(Goal goal);

    void delete(String goalId);
}
