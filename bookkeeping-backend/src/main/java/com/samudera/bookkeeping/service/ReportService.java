package com.samudera.bookkeeping.service;

import com.samudera.bookkeeping.context.UserContext;
import com.samudera.bookkeeping.dto.CategorySummaryVO;
import com.samudera.bookkeeping.dto.DailyTrendVO;
import com.samudera.bookkeeping.dto.StatsSummaryVO;
import com.samudera.bookkeeping.exception.BusinessException;
import com.samudera.bookkeeping.mapper.TransactionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Provides aggregated report data for charts and dashboards.
 */
@Service
public class ReportService {

    private final TransactionMapper transactionMapper;

    public ReportService(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
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
        return transactionMapper.getStats(userId);
    }
}
