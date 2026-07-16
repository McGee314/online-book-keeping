package com.samudera.bookkeeping.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides exchange rates for multi-currency conversion.
 * Fetches live rates from RapidAPI exchange-rates7, falls back to hardcoded rates.
 * All amounts are stored in CNY (base currency).
 *
 * Supported: CNY (base), USD, IDR, SGD, AUD, EUR, GBP, JPY, MAD, RUB
 */
@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);

    private static final String RAPIDAPI_URL = "https://exchange-rates7.p.rapidapi.com/convert";
    private static final String RAPIDAPI_HOST = "exchange-rates7.p.rapidapi.com";
    private static final String BASE_CURRENCY = "CNY";

    @Value("${rapidapi.key:7b773cba65mshe2f89cfcab86a82p1dcff1jsn5db02b8658c9}")
    private String rapidApiKey;

    // All supported target currencies
    private static final String[] TARGETS = {
        "USD", "IDR", "SGD", "AUD", "EUR", "GBP", "JPY", "MAD", "RUB"
    };

    // Fallback rates: 1 CNY = X target
    private static final Map<String, BigDecimal> FALLBACK_FROM_CNY = new HashMap<>();
    // Fallback rates: 1 target → CNY
    private static final Map<String, BigDecimal> FALLBACK_TO_CNY = new HashMap<>();

    static {
        putFallback("USD", 0.15, 6.67);
        putFallback("IDR", 2654, 0.000377);
        putFallback("SGD", 0.19, 5.26);
        putFallback("AUD", 0.21, 4.76);
        putFallback("EUR", 0.13, 7.69);
        putFallback("GBP", 0.11, 9.09);
        putFallback("JPY", 23.99, 0.0417);
        putFallback("MAD", 1.38, 0.725);
        putFallback("RUB", 11.54, 0.0867);
    }

    private static void putFallback(String code, double fromCny, double toCny) {
        FALLBACK_FROM_CNY.put(code, BigDecimal.valueOf(fromCny));
        FALLBACK_TO_CNY.put(code, BigDecimal.valueOf(toCny));
    }

    private final RestTemplate restTemplate;

    public CurrencyService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Returns exchange rates (1 base → target) for all supported currencies.
     */
    public Map<String, BigDecimal> getLiveRates(String baseCurrency) {
        String base = (baseCurrency != null ? baseCurrency : BASE_CURRENCY).toUpperCase();

        // Get CNY → all targets
        Map<String, BigDecimal> cnyToAll = fetchRatesFromCNY();

        if (BASE_CURRENCY.equals(base)) {
            return cnyToAll;
        }

        // Convert to requested base
        BigDecimal cnyToBase = cnyToAll.get(base);
        if (cnyToBase == null) return new HashMap<>();

        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : cnyToAll.entrySet()) {
            if (entry.getKey().equals(base)) continue;
            result.put(entry.getKey(),
                    entry.getValue().divide(cnyToBase, 10, RoundingMode.HALF_UP));
        }
        return result;
    }

    /**
     * Fetches 1 CNY → each target from RapidAPI, with fallback.
     */
    private Map<String, BigDecimal> fetchRatesFromCNY() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("x-rapidapi-host", RAPIDAPI_HOST);
        headers.set("x-rapidapi-key", rapidApiKey);

        Map<String, BigDecimal> rates = new HashMap<>();
        boolean anyLive = false;

        for (String target : TARGETS) {
            try {
                String url = RAPIDAPI_URL + "?base=" + BASE_CURRENCY + "&target=" + target;
                ResponseEntity<Map> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object convObj = response.getBody().get("convert_result");
                    if (convObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> conv = (Map<String, Object>) convObj;
                        Object rateVal = conv.get("rate");
                        if (rateVal instanceof Number) {
                            rates.put(target, BigDecimal.valueOf(((Number) rateVal).doubleValue()));
                            anyLive = true;
                        }
                    }
                }
            } catch (RestClientException e) {
                log.warn("RapidAPI failed for {}: {}", target, e.getMessage());
            }
        }

        if (anyLive) {
            log.info("Live rates from RapidAPI: {} of {} currencies", rates.size(), TARGETS.length);
            // Fill missing with fallback
            for (String target : TARGETS) {
                rates.putIfAbsent(target, FALLBACK_FROM_CNY.get(target));
            }
            return rates;
        }

        // All failed — full fallback
        log.info("RapidAPI unavailable, using fallback rates");
        return new HashMap<>(FALLBACK_FROM_CNY);
    }

    /**
     * Converts the given amount to the base currency (CNY).
     */
    public BigDecimal convertToBaseCurrency(BigDecimal amount, String fromCurrency) {
        if (amount == null) return BigDecimal.ZERO;
        if (fromCurrency == null || fromCurrency.equalsIgnoreCase(BASE_CURRENCY)) return amount;
        BigDecimal rate = FALLBACK_TO_CNY.get(fromCurrency.toUpperCase());
        if (rate == null) return amount;
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fetches exchange rate between two currencies.
     */
    public BigDecimal fetchRate(String fromCurrency, String toCurrency) {
        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();
        if (from.equals(to)) return BigDecimal.ONE;

        Map<String, BigDecimal> fromCny = fetchRatesFromCNY();
        if (BASE_CURRENCY.equals(from)) return fromCny.getOrDefault(to, BigDecimal.ONE);
        BigDecimal targetFromCny = fromCny.get(to);
        BigDecimal sourceFromCny = fromCny.get(from);
        if (targetFromCny != null && sourceFromCny != null && sourceFromCny.compareTo(BigDecimal.ZERO) > 0) {
            return targetFromCny.divide(sourceFromCny, 10, RoundingMode.HALF_UP);
        }
        return BigDecimal.ONE;
    }

    public BigDecimal fetchRateLive(String fromCurrency, String toCurrency) {
        return fetchRate(fromCurrency, toCurrency);
    }
}