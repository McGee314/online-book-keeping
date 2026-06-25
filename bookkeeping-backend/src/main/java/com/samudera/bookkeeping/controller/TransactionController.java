package com.samudera.bookkeeping.controller;

import com.samudera.bookkeeping.common.Result;
import com.samudera.bookkeeping.dto.TransactionQueryRequest;
import com.samudera.bookkeeping.dto.TransactionRequest;
import com.samudera.bookkeeping.entity.Transaction;
import com.samudera.bookkeeping.service.TransactionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public Result<List<Transaction>> list(@Valid TransactionQueryRequest request) {
        return Result.success(transactionService.listCurrentUserTransactions(request));
    }

    @PostMapping
    public Result<Transaction> create(@Valid @RequestBody TransactionRequest request) {
        return Result.success(transactionService.createTransaction(request));
    }

    @PutMapping("/{id}")
    public Result<Transaction> update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return Result.success(transactionService.updateTransaction(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return Result.success();
    }
}