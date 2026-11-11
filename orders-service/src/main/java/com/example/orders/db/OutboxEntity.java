package com.example.orders.db;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class OutboxEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OutboxEntity(UUID id, UUID orderId, String payload, OutboxStatus status, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public OutboxEntity(){}

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
