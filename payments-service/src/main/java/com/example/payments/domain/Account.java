package com.example.payments.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;

    public Account(UUID id, UUID userId, BigDecimal balance){
        if (id == null){
            throw new IllegalArgumentException("id не может быть пустым");
        }
        if (userId == null){
            throw new IllegalArgumentException("userId не может быть пустым");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("balance не может быть отрицательным");
        }
        this.id = id;
        this.userId = userId;
        this.balance = balance;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
