package com.budget.web.controller;

import com.budget.application.fire.FireQueryService;
import com.budget.application.fire.FireSettingsService;
import com.budget.web.dto.BudgetApiDtos;
import com.budget.web.dto.BudgetApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fire")
public class FireApiController {
    private final FireQueryService fireQueryService;
    private final FireSettingsService settingsService;
    private final BudgetApiMapper mapper;

    public FireApiController(FireQueryService fireQueryService, FireSettingsService settingsService, BudgetApiMapper mapper) {
        this.fireQueryService = fireQueryService;
        this.settingsService = settingsService;
        this.mapper = mapper;
    }

    @GetMapping("/summary")
    BudgetApiDtos.FireSummaryResponse summary() {
        return mapper.toFireSummary(fireQueryService.summary());
    }

    @GetMapping("/settings")
    BudgetApiDtos.FireSettingsDto settings() {
        return mapper.toFireSettings(settingsService.current());
    }

    @PutMapping("/settings")
    BudgetApiDtos.FireSettingsDto saveSettings(@RequestBody BudgetApiDtos.FireSettingsDto settings) {
        return mapper.toFireSettings(settingsService.save(mapper.toFireSettings(settings)));
    }
}
