package com.samudera.bookkeeping.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull(message = "must not be null")
    private Long categoryId;

    @NotNull(message = "must not be null")
    @Min(value = 1, message = "must be 1 or 2")
    @Max(value = 2, message = "must be 1 or 2")
    private Integer type;

    @NotNull(message = "must not be null")
    @DecimalMin(value = "0.01", message = "must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "must not be null")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate transactionDate;

    @Size(max = 10, message = "length must be less than or equal to 10")
    private String currencyCode;

    @Size(max = 500, message = "length must be less than or equal to 500")
    private String note;
}
