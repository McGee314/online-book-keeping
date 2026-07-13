package com.samudera.bookkeeping.dto;

import java.math.BigDecimal;

/**
 * Aggregated total per category for the donut chart.
 */
public class CategorySummaryVO {

    private String categoryName;
    private BigDecimal totalAmount;

    public CategorySummaryVO() {
    }

    public CategorySummaryVO(String categoryName, BigDecimal totalAmount) {
        this.categoryName = categoryName;
        this.totalAmount = totalAmount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}