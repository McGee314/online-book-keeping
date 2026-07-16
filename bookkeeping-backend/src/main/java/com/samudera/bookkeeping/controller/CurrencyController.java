package com.samudera.bookkeeping.controller;

import com.samudera.bookkeeping.common.Result;
import com.samudera.bookkeeping.service.CurrencyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Exposes live exchange rates so the frontend can display
 * real-time conversion data without hardcoded values.
 *
 * Supported: CNY (base) → USD.
 */
@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    /**
     * GET /api/currency/rates?base=CNY
     * Returns exchange rates for CNY against USD.
     */
    @GetMapping("/rates")
    public Result<Map<String, BigDecimal>> getRates(@RequestParam(defaultValue = "CNY") String base) {
        Map<String, BigDecimal> rates = currencyService.getLiveRates(base);
        return Result.success(rates);
    }
}