package com.example.payments.db;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountsRepository extends JpaRepository<AccountsEntity, UUID> {

    Optional<AccountsEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountsEntity a WHERE a.userId = :userId")
    Optional<AccountsEntity> findByUserIdForUpdate(
            @Param("userId") UUID userId);
}
