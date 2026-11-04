package com.example.payments.db;

import org.springframework.data.jpa.repository.JpaRepository;
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

//    @Modifying
//    @Query("""
//    UPDATE AccountsEntity a
//    SET a.balance = a.balance + :amount
//    WHERE a.userId = :userId
//    """)
//    int increaseBalanceByUserId(
//            @Param("userId") UUID userId,
//            @Param("amount") BigDecimal amount
//    );
}
