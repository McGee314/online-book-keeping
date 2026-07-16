package com.samudera.bookkeeping.dto;

import java.math.BigDecimal;

public class StatsSummaryVO {
    private BigDecimal income;
    private BigDecimal expense;
    private long transactionCount;
    private long categoryCount;
    private BigDecimal budget;
    private BigDecimal budgetPercent;

    public StatsSummaryVO() {}

    public StatsSummaryVO(BigDecimal income, BigDecimal expense, long transactionCount, long categoryCount) {
        this.income = income;
        this.expense = expense;
        this.transactionCount = transactionCount;
        this.categoryCount = categoryCount;
    }

    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }
    public BigDecimal getExpense() { return expense; }
    public void setExpense(BigDecimal expense) { this.expense = expense; }
    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }
    public long getCategoryCount() { return categoryCount; }
    public void setCategoryCount(long categoryCount) { this.categoryCount = categoryCount; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public BigDecimal getBudgetPercent() { return budgetPercent; }
    public void setBudgetPercent(BigDecimal budgetPercent) { this.budgetPercent = budgetPercent; }
}