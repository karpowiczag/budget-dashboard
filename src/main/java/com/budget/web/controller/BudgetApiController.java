package com.budget.web.controller;

import com.budget.application.importing.BudgetImportService;
import com.budget.application.importing.ImportSummary;
import com.budget.application.importing.TransactionImportFile;
import com.budget.application.reporting.AnalyticsReport;
import com.budget.application.reporting.BudgetQueryService;
import com.budget.application.reporting.CalendarReport;
import com.budget.application.reporting.TransactionPage;
import com.budget.application.reporting.TransactionQuery;
import com.budget.application.reporting.YearSummary;
import com.budget.domain.importjob.ImportRun;
import com.budget.domain.report.BudgetSnapshot;
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

    public BudgetApiController(BudgetQueryService queryService, BudgetImportService importService) {
        this.queryService = queryService;
        this.importService = importService;
    }

    @GetMapping("/session")
    SessionResponse session(Principal principal) {
        return new SessionResponse(principal != null, principal == null ? "" : principal.getName());
    }

    @GetMapping("/years")
    List<YearSummary> years() {
        return queryService.years();
    }

    @GetMapping("/reports/{year}/dashboard")
    BudgetSnapshot dashboard(@PathVariable @Min(2000) @Max(2100) int year) {
        return queryService.dashboard(year);
    }

    @GetMapping("/reports/{year}/calendar")
    CalendarReport calendar(
            @PathVariable @Min(2000) @Max(2100) int year,
            @RequestParam String month
    ) {
        return queryService.calendar(year, month);
    }

    @GetMapping("/reports/{year}/analytics")
    AnalyticsReport analytics(
            @PathVariable @Min(2000) @Max(2100) int year,
            @RequestParam(defaultValue = "year") String scope,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory
    ) {
        return queryService.analytics(year, scope, month, date, area, group, category, subcategory);
    }

    @GetMapping("/reports/{year}/transactions")
    TransactionPage transactions(
            @PathVariable @Min(2000) @Max(2100) int year,
            @RequestParam(defaultValue = "0") @Min(0) int page,
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
        return queryService.transactions(new TransactionQuery(
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
        ));
    }

    @PostMapping(path = "/imports/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImportSummary upload(@RequestParam("file") MultipartFile file) {
        return importService.importUpload(new TransactionImportFile(file.getOriginalFilename(), file.getSize(), file.getContentType(), file::getInputStream));
    }

    @PostMapping("/imports/rebuild")
    ImportSummary rebuild(@RequestParam(value = "year", required = false) @Min(2000) @Max(2100) Integer year) {
        return importService.rebuildLocal(year);
    }

    @GetMapping("/imports/runs")
    List<ImportRun> importRuns() {
        return queryService.importRuns();
    }

    public record SessionResponse(boolean authenticated, String name) {
    }
}
