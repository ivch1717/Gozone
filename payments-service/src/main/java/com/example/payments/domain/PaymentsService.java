package com.example.payments.domain;

import com.example.payments.db.AccountMapper;
import com.example.payments.db.AccountsEntity;
import com.example.payments.db.AccountsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentsService{
    private final AccountsRepository accountsRepository;
    private final AccountMapper mapper;

    public PaymentsService(AccountsRepository accountsRepository, AccountMapper mapper){
        this.accountsRepository = accountsRepository;
        this.mapper = mapper;
    }

    public Account createAccount(UUID userId) {
        AccountsEntity entity = new AccountsEntity(
                null,
                userId,
                BigDecimal.ZERO
        );

        AccountsEntity saved = accountsRepository.save(entity);

        return mapper.toDomain(saved);
    }

    public BigDecimal getBalance(UUID userId){
        return accountsRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Счёт пользователя не найден"))
                .getBalance();
    }
}
