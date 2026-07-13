package com.samudera.bookkeeping.controller;

import com.samudera.bookkeeping.common.Result;
import com.samudera.bookkeeping.dto.CategorySummaryVO;
import com.samudera.bookkeeping.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Provides aggregated data for dashboard charts.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * GET /api/reports/by-category?type=2
     * Returns per-category aggregated totals (base_amount).
     * type: 1 = Income, 2 = Expense (default)
     */
    @GetMapping("/by-category")
    public Result<List<CategorySummaryVO>> getByCategory(
            @RequestParam(defaultValue = "2") Integer type) {
        List<CategorySummaryVO> summary = reportService.getCategorySummary(type);
        return Result.success(summary);
    }
}