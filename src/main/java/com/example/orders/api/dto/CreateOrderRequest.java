package com.example.orders.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 64) String customerId,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull Item> items
) {
    public record Item(
            @NotBlank @Size(max = 64) String productId,
            @NotBlank @Size(max = 255) String productName,
            @NotNull @Min(1) @Max(10_000) Integer quantity,
            @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal unitPrice
    ) {}
}
