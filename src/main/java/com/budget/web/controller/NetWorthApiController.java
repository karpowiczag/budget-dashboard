package com.budget.web.controller;

import com.budget.application.networth.NetWorthService;
import com.budget.web.dto.BudgetApiDtos;
import com.budget.web.dto.BudgetApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/networth")
public class NetWorthApiController {
    private final NetWorthService service;
    private final BudgetApiMapper mapper;

    public NetWorthApiController(NetWorthService service, BudgetApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    BudgetApiDtos.NetWorthResponse netWorth() {
        return mapper.toNetWorth(service.overview());
    }

    @PutMapping("/accounts/{key}")
    BudgetApiDtos.NetWorthResponse saveAccount(@PathVariable String key, @RequestBody BudgetApiDtos.AccountUpsertRequest request) {
        return mapper.toNetWorth(service.saveAccount(mapper.toAccount(key, request)));
    }
}
