package com.samudera.bookkeeping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samudera.bookkeeping.dto.TransactionQueryRequest;
import com.samudera.bookkeeping.dto.TransactionRequest;
import com.samudera.bookkeeping.entity.Transaction;

import java.util.List;

public interface TransactionService extends IService<Transaction> {

    List<Transaction> listCurrentUserTransactions(TransactionQueryRequest request);

    Transaction createTransaction(TransactionRequest request);

    Transaction updateTransaction(Long id, TransactionRequest request);

    void deleteTransaction(Long id);
}