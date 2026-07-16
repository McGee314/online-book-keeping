package com.samudera.bookkeeping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("budgets")
public class Budget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("budget_month")
    private String budgetMonth;

    @TableField("amount")
    private BigDecimal amount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}