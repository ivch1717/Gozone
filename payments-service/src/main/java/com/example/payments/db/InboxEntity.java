package com.example.payments.db;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox")
public class InboxEntity {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public InboxEntity() {
    }

    public InboxEntity(UUID eventId, UUID orderId, String payload, InboxStatus status, Instant createdAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getPayload() {
        return payload;
    }

    public InboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setStatus(InboxStatus status) {
        this.status = status;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
