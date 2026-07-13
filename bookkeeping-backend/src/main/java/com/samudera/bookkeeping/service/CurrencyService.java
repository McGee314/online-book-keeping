package com.samudera.bookkeeping.service;

import com.samudera.bookkeeping.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches live exchange rates from the free Frankfurter API
 * and provides conversion utilities.
 */
@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);

    private static final String FRANKFURTER_URL =
            "https://api.frankfurter.dev/v1/latest?from={from}&to={to}";

    private static final String BASE_CURRENCY = "IDR";

    private final RestTemplate restTemplate;

    public CurrencyService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Returns live exchange rates from Frankfurter API for the specified base currency.
     * Used by the frontend to display conversion rates.
     * Throws if the API is unavailable — the frontend should show an error, not fake numbers.
     */
    public Map<String, BigDecimal> getLiveRates(String baseCurrency) {
        String base = (baseCurrency != null ? baseCurrency : BASE_CURRENCY).toUpperCase();
        if (!BASE_CURRENCY.equals(base)) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("CNY", fetchRateLive(BASE_CURRENCY, "CNY"));
        rates.put("USD", fetchRateLive(BASE_CURRENCY, "USD"));
        return rates;
    }

    /**
     * Converts the given amount from {@code fromCurrency} to the system base currency (IDR).
     * If the amount is already in IDR, returns it unchanged.
     *
     * @param amount       original amount (positive)
     * @param fromCurrency currency code e.g. CNY, USD
     * @return the converted amount in IDR
     */
    public BigDecimal convertToBaseCurrency(BigDecimal amount, String fromCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (fromCurrency == null || fromCurrency.equalsIgnoreCase(BASE_CURRENCY)) {
            return amount;
        }
        BigDecimal rate = fetchRate(fromCurrency, BASE_CURRENCY);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fetches the exchange rate from Frankfurter API.
     * Falls back to hardcoded approximate rates if the API is unavailable.
     * Used internally for transaction base-amount storage — never blocks user operations.
     */
    BigDecimal fetchRate(String fromCurrency, String toCurrency) {
        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();

        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        try {
            BigDecimal live = callFrankfurterApi(from, to);
            if (live != null) {
                return live;
            }
        } catch (RestClientException e) {
            log.warn("Frankfurter API unavailable ({}) — using fallback rates", e.getMessage());
        }

        return getFallbackRate(from, to);
    }

    /**
     * Fetches the exchange rate from Frankfurter API without fallback.
     * Throws BusinessException if the API is down — used for frontend display only.
     */
    BigDecimal fetchRateLive(String fromCurrency, String toCurrency) {
        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();

        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        try {
            BigDecimal live = callFrankfurterApi(from, to);
            if (live != null) {
                return live;
            }
        } catch (RestClientException e) {
            log.warn("Frankfurter API unavailable for live rates display: {}", e.getMessage());
        }

        throw new BusinessException(503, "Exchange rate service temporarily unavailable");
    }

    /**
     * Calls the Frankfurter API and returns the rate, or null on unexpected response.
     */
    private BigDecimal callFrankfurterApi(String from, String to) {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                FRANKFURTER_URL,
                Map.class,
                from,
                to
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            Object ratesObj = body.get("rates");
            if (ratesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) ratesObj;
                Object rateValue = rates.get(to);
                if (rateValue instanceof Number) {
                    BigDecimal liveRate = BigDecimal.valueOf(((Number) rateValue).doubleValue());
                    log.info("Live rate from Frankfurter: 1 {} = {} {}", from, liveRate, to);
                    return liveRate;
                }
            }
        }
        log.warn("Frankfurter API returned unexpected response: {}", response.getBody());
        return null;
    }

    private BigDecimal getFallbackRate(String from, String to) {
        // Rates updated ~July 2026 approximations
        switch (from) {
            case "CNY":
                switch (to) {
                    case "IDR": return new BigDecimal("2668.00");
                    case "USD": return new BigDecimal("0.14");
                }
                break;
            case "USD":
                switch (to) {
                    case "IDR": return new BigDecimal("16200.00");
                    case "CNY": return new BigDecimal("7.14");
                }
                break;
            case "IDR":
                switch (to) {
                    case "CNY": return new BigDecimal("0.000375");
                    case "USD": return new BigDecimal("0.000062");
                }
                break;
        }
        throw new BusinessException(500, "Unsupported currency conversion: " + from + " → " + to);
    }
}