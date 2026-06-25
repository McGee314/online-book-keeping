package com.samudera.bookkeeping.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CategoryRequest {

    @NotBlank(message = "must not be blank")
    @Size(max = 50, message = "length must be less than or equal to 50")
    private String name;

    @NotNull(message = "must not be null")
    @Min(value = 1, message = "must be 1 or 2")
    @Max(value = 2, message = "must be 1 or 2")
    private Integer type;

    @Size(max = 100, message = "length must be less than or equal to 100")
    private String icon;

    @Size(max = 20, message = "length must be less than or equal to 20")
    private String color;

    private Integer sortOrder;
}