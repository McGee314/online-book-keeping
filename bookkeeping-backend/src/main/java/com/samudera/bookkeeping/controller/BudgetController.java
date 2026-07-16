package com.samudera.bookkeeping.controller;

import com.samudera.bookkeeping.common.Result;
import com.samudera.bookkeeping.dto.BudgetRequest;
import com.samudera.bookkeeping.entity.Budget;
import com.samudera.bookkeeping.service.BudgetService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public Result<Budget> getCurrentMonthBudget() {
        Budget budget = budgetService.getCurrentMonthBudget();
        return Result.success(budget);
    }

    @PostMapping
    public Result<Budget> setBudget(@Valid @RequestBody BudgetRequest request) {
        Budget budget = budgetService.setBudget(request);
        return Result.success(budget);
    }
}