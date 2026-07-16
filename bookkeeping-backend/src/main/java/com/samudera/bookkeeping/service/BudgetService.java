package com.samudera.bookkeeping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.samudera.bookkeeping.context.UserContext;
import com.samudera.bookkeeping.dto.BudgetRequest;
import com.samudera.bookkeeping.entity.Budget;
import com.samudera.bookkeeping.mapper.BudgetMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class BudgetService {

    private final BudgetMapper budgetMapper;

    public BudgetService(BudgetMapper budgetMapper) {
        this.budgetMapper = budgetMapper;
    }

    /**
     * Get the overall monthly budget for the current user and current month.
     * Returns null if no budget is set.
     */
    public Budget getCurrentMonthBudget() {
        Long userId = UserContext.getUserId();
        String yearMonth = currentYearMonth();
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getUserId, userId)
               .eq(Budget::getBudgetMonth, yearMonth)
               .isNull(Budget::getCategoryId);
        return budgetMapper.selectOne(wrapper);
    }

    /**
     * Set or update the overall monthly budget for the current user and current month.
     */
    public Budget setBudget(BudgetRequest request) {
        Long userId = UserContext.getUserId();
        String yearMonth = currentYearMonth();

        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getUserId, userId)
               .eq(Budget::getBudgetMonth, yearMonth)
               .isNull(Budget::getCategoryId);
        Budget existing = budgetMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setAmount(request.getAmount());
            budgetMapper.updateById(existing);
            return existing;
        } else {
            Budget budget = new Budget();
            budget.setUserId(userId);
            budget.setCategoryId(null); // overall budget
            budget.setBudgetMonth(yearMonth);
            budget.setAmount(request.getAmount());
            budgetMapper.insert(budget);
            return budget;
        }
    }

    private String currentYearMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}