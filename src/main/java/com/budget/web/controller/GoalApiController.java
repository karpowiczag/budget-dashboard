package com.budget.web.controller;

import com.budget.application.goal.GoalService;
import com.budget.web.dto.BudgetApiDtos;
import com.budget.web.dto.BudgetApiMapper;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalApiController {
    private final GoalService service;
    private final BudgetApiMapper mapper;

    public GoalApiController(GoalService service, BudgetApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    List<BudgetApiDtos.GoalResponse> listGoals() {
        return service.list().stream().map(mapper::toGoal).toList();
    }

    @PutMapping("/{id}")
    List<BudgetApiDtos.GoalResponse> updateGoal(@PathVariable String id, @RequestBody BudgetApiDtos.GoalUpsertRequest request) {
        return service.save(mapper.toGoal(id, request)).stream().map(mapper::toGoal).toList();
    }

    @DeleteMapping("/{id}")
    List<BudgetApiDtos.GoalResponse> deleteGoal(@PathVariable String id) {
        return service.delete(id).stream().map(mapper::toGoal).toList();
    }
}
