package com.example.orders.api.dtos;

import com.example.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String name,
        BigDecimal amount,
        String description,
        OrderStatus status
) {
}
