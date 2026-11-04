package com.example.orders.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class Order {
    private UUID id;
    private UUID userId;
    private String name;
    private BigDecimal amount;
    private String description;
    private OrderStatus status;


    public Order(UUID id, UUID userId, String name, BigDecimal amount, OrderStatus status, String description) {
        if (id == null){
            throw new IllegalArgumentException("id не может быть пустым");
        }
        if (userId == null){
            throw new IllegalArgumentException("userId не может быть пустым");
        }
        if (amount == null || amount.signum() <= 0){
            throw new IllegalArgumentException("стоимость должна быть положительной");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("имя заказа не может быть пустым");
        }
        if (status == null){
            throw new IllegalArgumentException("статус заказа не может быть пустым");
        }
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.amount = amount;
        this.status = status;
        this.description = description;
    }


    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void markCanceled() {
        if (status == OrderStatus.CANCELED) {
            return;
        }
        if (status != OrderStatus.NEW) {
            throw new IllegalStateException(
                    "Нельзя отменить заказ в статусе " + status);
        }
        status = OrderStatus.CANCELED;
    }

    public void markPaid(){
        if (status == OrderStatus.PAID) {
            return;
        }
        if (status != OrderStatus.NEW) {
            throw new IllegalStateException(
                    "Нельзя оплатить заказ в статусе " + status);
        }
        status = OrderStatus.PAID;
    }
}
