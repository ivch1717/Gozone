package com.example.payments.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InboxRepository extends JpaRepository<InboxEntity, UUID> {
}
