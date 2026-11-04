package com.example.orders.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID userId,
        @NotNull @NotBlank @Size(max = 200) String name,
        @NotNull @Positive BigDecimal amount,
        String description) {
}
