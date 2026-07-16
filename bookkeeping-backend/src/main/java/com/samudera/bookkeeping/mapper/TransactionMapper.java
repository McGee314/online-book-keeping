package com.samudera.bookkeeping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samudera.bookkeeping.dto.CategorySummaryVO;
import com.samudera.bookkeeping.dto.DailyTrendVO;
import com.samudera.bookkeeping.dto.StatsSummaryVO;
import com.samudera.bookkeeping.entity.Transaction;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TransactionMapper extends BaseMapper<Transaction> {

    /**
     * Aggregate base_amount grouped by category for the given user and type.
     * Uses base_amount for accurate multi-currency totals.
     */
    @Select("SELECT c.name AS categoryName, COALESCE(SUM(t.base_amount), 0) AS totalAmount " +
            "FROM transactions t " +
            "JOIN categories c ON t.category_id = c.id " +
            "WHERE t.user_id = #{userId} AND t.type = #{type} AND t.deleted = 0 " +
            "GROUP BY c.id, c.name " +
            "ORDER BY totalAmount DESC")
    List<CategorySummaryVO> aggregateByCategory(@Param("userId") Long userId, @Param("type") Integer type);

    /**
     * Daily income and expense totals for the given date range.
     * Frontend always provides start/end dates (defaults to last 7 days).
     */
    @Select("SELECT t.transaction_date AS date, " +
            "COALESCE(SUM(CASE WHEN t.type = 1 THEN t.base_amount ELSE 0 END), 0) AS income, " +
            "COALESCE(SUM(CASE WHEN t.type = 2 THEN t.base_amount ELSE 0 END), 0) AS expense " +
            "FROM transactions t " +
            "WHERE t.user_id = #{userId} AND t.deleted = 0 " +
            "AND t.transaction_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY t.transaction_date " +
            "ORDER BY t.transaction_date ASC")
    List<DailyTrendVO> dailyTrend(@Param("userId") Long userId,
                                   @Param("startDate") java.time.LocalDate startDate,
                                   @Param("endDate") java.time.LocalDate endDate);

    /**
     * Get total income, expense, and transaction count for the user across ALL transactions.
     */
    @Select("SELECT " +
            "COALESCE(SUM(CASE WHEN t.type = 1 THEN t.base_amount ELSE 0 END), 0) AS income, " +
            "COALESCE(SUM(CASE WHEN t.type = 2 THEN t.base_amount ELSE 0 END), 0) AS expense, " +
            "COUNT(*) AS transactionCount, " +
            "0 AS categoryCount " +
            "FROM transactions t " +
            "WHERE t.user_id = #{userId} AND t.deleted = 0")
    StatsSummaryVO getStats(@Param("userId") Long userId);
}
