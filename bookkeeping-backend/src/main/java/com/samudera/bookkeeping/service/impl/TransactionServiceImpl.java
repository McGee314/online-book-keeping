package com.samudera.bookkeeping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samudera.bookkeeping.context.UserContext;
import com.samudera.bookkeeping.dto.TransactionQueryRequest;
import com.samudera.bookkeeping.dto.TransactionRequest;
import com.samudera.bookkeeping.entity.Category;
import com.samudera.bookkeeping.entity.Transaction;
import com.samudera.bookkeeping.exception.BusinessException;
import com.samudera.bookkeeping.mapper.TransactionMapper;
import com.samudera.bookkeeping.service.CategoryService;
import com.samudera.bookkeeping.service.CurrencyService;
import com.samudera.bookkeeping.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionServiceImpl extends ServiceImpl<TransactionMapper, Transaction> implements TransactionService {

    private final CategoryService categoryService;
    private final CurrencyService currencyService;

    public TransactionServiceImpl(CategoryService categoryService, CurrencyService currencyService) {
        this.categoryService = categoryService;
        this.currencyService = currencyService;
    }

    @Override
    public List<Transaction> listCurrentUserTransactions(TransactionQueryRequest request) {
        Long userId = requireCurrentUserId();
        validateDateRange(request);

        LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getUserId, userId)
                .orderByDesc(Transaction::getTransactionDate)
                .orderByDesc(Transaction::getId);

        if (request.getType() != null) {
            wrapper.eq(Transaction::getType, request.getType());
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Transaction::getCategoryId, request.getCategoryId());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(Transaction::getTransactionDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(Transaction::getTransactionDate, request.getEndDate());
        }

        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Transaction createTransaction(TransactionRequest request) {
        Long userId = requireCurrentUserId();
        Category category = categoryService.getOwnedCategoryOrThrow(request.getCategoryId());
        validateCategoryType(category, request.getType());

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setCategoryId(request.getCategoryId());
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());

        // Currency handling
        String currencyCode = request.getCurrencyCode() != null
                ? request.getCurrencyCode().toUpperCase() : "IDR";
        transaction.setCurrencyCode(currencyCode);
        BigDecimal baseAmount = currencyService.convertToBaseCurrency(request.getAmount(), currencyCode);
        transaction.setBaseAmount(baseAmount);

        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNote(request.getNote());
        transaction.setDeleted(0);
        save(transaction);
        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Transaction updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = getOwnedTransactionOrThrow(id);
        Category category = categoryService.getOwnedCategoryOrThrow(request.getCategoryId());
        validateCategoryType(category, request.getType());

        transaction.setCategoryId(request.getCategoryId());
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());

        String currencyCode = request.getCurrencyCode() != null
                ? request.getCurrencyCode().toUpperCase() : "IDR";
        transaction.setCurrencyCode(currencyCode);
        BigDecimal baseAmount = currencyService.convertToBaseCurrency(request.getAmount(), currencyCode);
        transaction.setBaseAmount(baseAmount);

        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNote(request.getNote());
        updateById(transaction);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransaction(Long id) {
        getOwnedTransactionOrThrow(id);
        removeById(id);
    }

    private Transaction getOwnedTransactionOrThrow(Long id) {
        Long userId = requireCurrentUserId();
        Transaction transaction = getOne(new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getId, id)
                .eq(Transaction::getUserId, userId)
                .last("LIMIT 1"));
        if (transaction == null) {
            throw new BusinessException(404, "Transaction not found");
        }
        return transaction;
    }

    private void validateCategoryType(Category category, Integer type) {
        if (!category.getType().equals(type)) {
            throw new BusinessException(400, "Transaction type must match category type");
        }
    }

    private void validateDateRange(TransactionQueryRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(400, "startDate cannot be after endDate");
        }
    }

    private Long requireCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "Unauthorized");
        }
        return userId;
    }
}