package com.example.orders.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {
    Optional<OutboxEntity> findFirstByStatusOrderByCreatedAtAsc(
            OutboxStatus status);
}