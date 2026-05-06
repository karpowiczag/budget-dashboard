package com.budget.web.controller;

import com.budget.application.importing.BudgetImportService;
import com.budget.application.importing.TransactionImportFile;
import com.budget.application.reporting.BudgetQueryService;
import com.budget.application.reporting.TransactionQuery;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.web.dto.BudgetApiDtos;
import com.budget.web.dto.BudgetApiMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1")
public class BudgetApiController {
    private final BudgetQueryService queryService;
    private final BudgetImportService importService;
    private final BudgetSettingsService settingsService;
    private final BudgetApiMapper mapper;

    public BudgetApiController(
            BudgetQueryService queryService,
            BudgetImportService importService,
            BudgetSettingsService settingsService,
            BudgetApiMapper mapper
    ) {
        this.queryService = queryService;
        this.importService = importService;
        this.settingsService = settingsService;
        this.mapper = mapper;
    }

    @GetMapping("/session")
    BudgetApiDtos.SessionResponse session(Principal principal) {
        return new BudgetApiDtos.SessionResponse(principal != null, principal == null ? "" : principal.getName());
    }

    @GetMapping("/years")
    List<BudgetApiDtos.YearSummaryResponse> years() {
        return queryService.years().stream().map(mapper::toYearSummary).toList();
    }

    @GetMapping("/reports/{year}/dashboard")
    BudgetApiDtos.DashboardResponse dashboard(@PathVariable @Min(2000) @Max(2100) int year) {
        return mapper.toDashboard(queryService.dashboard(year));
    }

    @GetMapping("/reports/{year}/calendar")
    BudgetApiDtos.CalendarResponse calendar(
            @PathVariable @Min(2000) @Max(2100) int year,
            @RequestParam String month
    ) {
        return mapper.toCalendar(queryService.calendar(year, month));
    }

    @GetMapping("/reports/{year}/analytics")
    BudgetApiDtos.AnalyticsResponse analytics(
            @PathVariable @Min(2000) @Max(2100) int year,
            @RequestParam(defaultValue = "year") String scope,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory
    ) {
        return mapper.toAnalytics(queryService.analytics(year, scope, month, date, area, group, category, subcategory));
    }

    @GetMapping("/reports/{year}/transactions")
    BudgetApiDtos.TransactionPageResponse transactions(
            @PathVariable @Min(2000) @Max(2100) int year,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "postedDate,desc") String sort,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String bucket,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory
    ) {
        return mapper.toTransactionPage(queryService.transactions(new TransactionQuery(
                year,
                page,
                size,
                sort,
                month,
                date,
                query,
                bucket,
                area,
                group,
                category,
                subcategory
        )));
    }

    @PostMapping(path = "/imports/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    BudgetApiDtos.ImportSummaryResponse upload(@RequestParam("file") MultipartFile file) {
        return mapper.toImportSummary(importService.importUpload(new TransactionImportFile(file.getOriginalFilename(), file.getSize(), file.getContentType(), file::getInputStream)));
    }

    @PostMapping("/imports/rebuild")
    BudgetApiDtos.ImportSummaryResponse rebuild(@RequestParam(value = "year", required = false) @Min(2000) @Max(2100) Integer year) {
        return mapper.toImportSummary(importService.rebuildLocal(year));
    }

    @GetMapping("/imports/runs")
    List<BudgetApiDtos.ImportRunResponse> importRuns() {
        return queryService.importRuns().stream().map(mapper::toImportRun).toList();
    }

    @GetMapping("/settings/budget")
    BudgetApiDtos.BudgetSettingsDto budgetSettings() {
        return mapper.toBudgetSettings(settingsService.current());
    }

    @PutMapping("/settings/budget")
    BudgetApiDtos.BudgetSettingsDto saveBudgetSettings(@org.springframework.web.bind.annotation.RequestBody BudgetApiDtos.BudgetSettingsDto settings) {
        return mapper.toBudgetSettings(settingsService.save(mapper.toBudgetSettings(settings)));
    }
}
