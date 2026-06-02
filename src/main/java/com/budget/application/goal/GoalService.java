package com.budget.application.goal;

import com.budget.domain.goal.Goal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Thin use case over goal persistence. Mutations return the full, ordered goal list so the
 * client refreshes in one round-trip (same shape as the net-worth endpoints).
 */
@Service
public class GoalService {
    private final GoalStore store;

    public GoalService(GoalStore store) {
        this.store = store;
    }

    public List<Goal> list() {
        return store.goals();
    }

    public List<Goal> save(Goal goal) {
        store.save(goal);
        return store.goals();
    }

    public List<Goal> delete(String goalId) {
        store.delete(goalId);
        return store.goals();
    }
}
