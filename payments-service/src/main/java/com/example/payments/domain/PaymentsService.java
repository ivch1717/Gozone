package com.example.payments.domain;

import com.example.payments.db.AccountDbMapper;
import com.example.payments.db.AccountsEntity;
import com.example.payments.db.AccountsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentsService{
    private static final Logger log = LoggerFactory.getLogger(PaymentsService.class);

    private final AccountsRepository accountsRepository;
    private final AccountDbMapper mapper;

    public PaymentsService(AccountsRepository accountsRepository, AccountDbMapper mapper){
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

        Account account = mapper.toDomain(saved);
        log.info("Счёт создан. accountId={}", account.getId());
        return account;
    }

    public BigDecimal getBalance(UUID userId){
        AccountsEntity account = accountsRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Счёт пользователя не найден"));

        log.debug("Получен баланс счёта. accountId={}", account.getId());
        return account.getBalance();
    }

    public Account topUpAccount(UUID userId, BigDecimal amount) {
        AccountsEntity entity = accountsRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Счёт пользователя не найден"));

        Account domain = mapper.toDomain(entity);
        domain.topUp(amount);

        log.info("Принят запрос на пополнение счёта. accountId={}", entity.getId());
        entity.setBalance(domain.getBalance());
        accountsRepository.save(entity);
        log.info("Счёт пополнен. accountId={}", entity.getId());
        return domain;
    }
}
