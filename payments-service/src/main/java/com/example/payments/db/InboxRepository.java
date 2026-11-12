package com.example.payments.db;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface InboxRepository extends JpaRepository<InboxEntity, UUID> {
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO inbox (
                event_id, order_id, payload, status, created_at
            )
            VALUES (
                :eventId, :orderId, :payload, 'NEW', :createdAt
            )
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("orderId") UUID orderId,
            @Param("payload") String payload,
            @Param("createdAt") Instant createdAt);
}
