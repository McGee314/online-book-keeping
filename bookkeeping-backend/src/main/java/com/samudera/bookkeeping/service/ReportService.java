package com.samudera.bookkeeping.service;

import com.samudera.bookkeeping.context.UserContext;
import com.samudera.bookkeeping.dto.CategorySummaryVO;
import com.samudera.bookkeeping.dto.DailyTrendVO;
import com.samudera.bookkeeping.dto.StatsSummaryVO;
import com.samudera.bookkeeping.entity.Budget;
import com.samudera.bookkeeping.exception.BusinessException;
import com.samudera.bookkeeping.mapper.TransactionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Provides aggregated report data for charts and dashboards.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final TransactionMapper transactionMapper;
    private final BudgetService budgetService;

    public ReportService(TransactionMapper transactionMapper, BudgetService budgetService) {
        this.transactionMapper = transactionMapper;
        this.budgetService = budgetService;
    }

    /**
     * Returns per-category totals for the given transaction type.
     * @param type 1 = Income, 2 = Expense
     */
    public List<CategorySummaryVO> getCategorySummary(Integer type) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "Unauthorized");
        }
        return transactionMapper.aggregateByCategory(userId, type);
    }

    /**
     * Returns daily income and expense totals for the last 7 days.
     */
    public List<DailyTrendVO> getDailyTrend(LocalDate startDate, LocalDate endDate) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "Unauthorized");
        }
        return transactionMapper.dailyTrend(userId, startDate, endDate);
    }

    /**
     * Returns total income, expense, counts — from ALL transactions in DB.
     */
    public StatsSummaryVO getStats() {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(401, "Unauthorized");
        StatsSummaryVO stats = transactionMapper.getStats(userId);

        // Enrich with budget info (gracefully handle missing table / SQL errors)
        try {
            Budget budget = budgetService.getCurrentMonthBudget();
            if (budget != null && budget.getAmount() != null) {
                stats.setBudget(budget.getAmount());
                BigDecimal expense = stats.getExpense() != null ? stats.getExpense() : BigDecimal.ZERO;
                if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percent = expense.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                                              .multiply(BigDecimal.valueOf(100))
                                              .setScale(1, RoundingMode.HALF_UP);
                    stats.setBudgetPercent(percent);
                } else {
                    stats.setBudgetPercent(BigDecimal.ZERO);
                }
            }
        } catch (Exception e) {
            // Budget table may not exist yet — just skip budget info
            log.warn("Could not load budget: {}", e.getMessage());
        }

        return stats;
    }
}
