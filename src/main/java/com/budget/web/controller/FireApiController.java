package com.budget.web.controller;

import com.budget.application.fire.FireQueryService;
import com.budget.web.dto.BudgetApiDtos;
import com.budget.web.dto.BudgetApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fire")
public class FireApiController {
    private final FireQueryService fireQueryService;
    private final BudgetApiMapper mapper;

    public FireApiController(FireQueryService fireQueryService, BudgetApiMapper mapper) {
        this.fireQueryService = fireQueryService;
        this.mapper = mapper;
    }

    @GetMapping("/summary")
    BudgetApiDtos.FireSummaryResponse summary() {
        return mapper.toFireSummary(fireQueryService.summary());
    }
}
