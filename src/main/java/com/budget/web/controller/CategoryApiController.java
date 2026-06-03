package com.budget.web.controller;

import com.budget.application.categorization.CategoryCatalogService;
import com.budget.application.categorization.CategoryRuleService;
import com.budget.web.dto.BudgetApiDtos;
import com.budget.web.dto.BudgetApiMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User-owned category catalog API (Phase 3b): add / rename / hide / reorder categories and rename
 * groups. Hide is {@code PUT {id}} with {@code archived: true}; there is no hard delete in 3b.
 * Every operation returns the full refreshed catalog so the client updates in one round-trip.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryApiController {
    private final CategoryCatalogService service;
    private final CategoryRuleService ruleService;
    private final BudgetApiMapper mapper;

    public CategoryApiController(CategoryCatalogService service, CategoryRuleService ruleService, BudgetApiMapper mapper) {
        this.service = service;
        this.ruleService = ruleService;
        this.mapper = mapper;
    }

    @GetMapping
    BudgetApiDtos.CategoriesResponse listCategories() {
        return current();
    }

    @PutMapping("/{id}")
    BudgetApiDtos.CategoriesResponse updateCategory(@PathVariable String id, @RequestBody BudgetApiDtos.CategoryUpsertRequest request) {
        service.saveCategory(mapper.toCategory(id, request));
        return current();
    }

    @DeleteMapping("/{id}")
    BudgetApiDtos.CategoriesResponse deleteCategory(@PathVariable String id, @RequestParam(required = false) String reassignTo) {
        service.delete(id, reassignTo);
        return current();
    }

    @PutMapping("/groups/{id}")
    BudgetApiDtos.CategoriesResponse updateCategoryGroup(@PathVariable String id, @RequestBody BudgetApiDtos.CategoryGroupUpsertRequest request) {
        service.saveGroup(mapper.toCategoryGroup(id, request));
        return current();
    }

    @PostMapping("/reorder")
    BudgetApiDtos.CategoriesResponse reorderCategories(@RequestBody BudgetApiDtos.CategoryReorderRequest request) {
        service.reorder(request.groupIds(), request.categoryIds());
        return current();
    }

    @PutMapping("/rules/{id}")
    BudgetApiDtos.CategoriesResponse updateCategoryRule(@PathVariable String id, @RequestBody BudgetApiDtos.RuleUpsertRequest request) {
        ruleService.saveRule(mapper.toClassificationRule(id, request));
        return current();
    }

    @DeleteMapping("/rules/{id}")
    BudgetApiDtos.CategoriesResponse deleteCategoryRule(@PathVariable String id) {
        ruleService.deleteRule(id);
        return current();
    }

    private BudgetApiDtos.CategoriesResponse current() {
        return mapper.toCategories(service.groups(), service.categories(), ruleService.rules());
    }
}
