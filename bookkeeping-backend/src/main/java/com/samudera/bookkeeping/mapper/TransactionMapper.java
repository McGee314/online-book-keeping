package com.samudera.bookkeeping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.samudera.bookkeeping.dto.CategorySummaryVO;
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
}
