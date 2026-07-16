package com.samudera.bookkeeping.controller;

import com.samudera.bookkeeping.common.Result;
import com.samudera.bookkeeping.dto.CategorySummaryVO;
import com.samudera.bookkeeping.dto.DailyTrendVO;
import com.samudera.bookkeeping.dto.StatsSummaryVO;
import com.samudera.bookkeeping.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
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

    /**
     * GET /api/reports/daily-trend
     * Returns daily income/expense totals for the last 7 days.
     */
    @GetMapping("/daily-trend")
    public Result<List<DailyTrendVO>> getDailyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DailyTrendVO> trend = reportService.getDailyTrend(startDate, endDate);
        return Result.success(trend);
    }

    /**
     * GET /api/reports/stats
     * Returns total income/expense from all transactions in DB.
     */
    @GetMapping("/stats")
    public Result<StatsSummaryVO> getStats() {
        StatsSummaryVO stats = reportService.getStats();
        return Result.success(stats);
    }
}
