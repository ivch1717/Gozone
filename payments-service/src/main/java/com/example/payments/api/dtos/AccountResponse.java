package com.example.payments.api.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID userId,
        BigDecimal balance
) {
}
