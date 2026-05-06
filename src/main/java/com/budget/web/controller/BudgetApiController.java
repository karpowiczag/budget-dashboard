package com.budget.web.controller;

import com.budget.application.importing.BudgetImportService;
import com.budget.application.reporting.BudgetQueryService;
import com.budget.application.importing.ImportSummary;
import com.budget.application.importing.TransactionImportFile;
import com.budget.domain.importjob.ImportRun;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class BudgetApiController {
    private final BudgetQueryService queryService;
    private final BudgetImportService importService;

    public BudgetApiController(BudgetQueryService queryService, BudgetImportService importService) {
        this.queryService = queryService;
        this.importService = importService;
    }

    @GetMapping("/api/me")
    Map<String, Object> me(Principal principal) {
        return Map.of("authenticated", principal != null, "name", principal == null ? "" : principal.getName());
    }

    @GetMapping("/api/years")
    List<Map<String, Object>> years() {
        return queryService.years();
    }

    @GetMapping("/api/budget/{year}")
    Map<String, Object> budget(@PathVariable @Min(2000) @Max(2100) int year) {
        return queryService.budget(year);
    }

    @GetMapping("/api/import-runs")
    List<ImportRun> importRuns() {
        return queryService.importRuns();
    }

    @PostMapping(path = "/api/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImportSummary upload(@RequestParam("file") MultipartFile file) {
        return importService.importUpload(new TransactionImportFile(file.getOriginalFilename(), file.getSize(), file::getInputStream));
    }

    @PostMapping("/api/rebuild")
    ImportSummary rebuild(@RequestParam(value = "year", required = false) @Min(2000) @Max(2100) Integer year) {
        return importService.rebuildLocal(year);
    }
}
