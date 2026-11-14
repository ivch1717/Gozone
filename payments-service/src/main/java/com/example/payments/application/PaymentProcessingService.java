package com.example.payments.application;

import com.example.payments.db.AccountDbMapper;
import com.example.payments.db.AccountsEntity;
import com.example.payments.db.AccountsRepository;
import com.example.payments.db.InboxEntity;
import com.example.payments.db.InboxRepository;
import com.example.payments.db.InboxStatus;
import com.example.payments.db.OutboxEntity;
import com.example.payments.db.OutboxRepository;
import com.example.payments.db.OutboxStatus;
import com.example.payments.domain.Account;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentProcessingService {

    private final InboxRepository inboxRepository;
    private final AccountsRepository accountsRepository;
    private final AccountDbMapper accountDbMapper;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    public PaymentProcessingService(
            InboxRepository inboxRepository,
            AccountsRepository accountsRepository,
            AccountDbMapper accountDbMapper,
            ObjectMapper objectMapper,
            OutboxRepository outboxRepository) {

        this.inboxRepository = inboxRepository;
        this.accountsRepository = accountsRepository;
        this.accountDbMapper = accountDbMapper;
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void processNext() {
        Optional<InboxEntity> pending =
                inboxRepository.findNextForProcessing();

        if (pending.isEmpty()) {
            return;
        }

        InboxEntity event = pending.orElseThrow();

        PaymentRequest request = objectMapper.readValue(
                event.getPayload(),
                PaymentRequest.class);

        if (request == null
                || request.userId() == null
                || request.amount() == null
                || request.amount().signum() <= 0) {

            throw new IllegalArgumentException(
                    "Некорректная заявка на оплату");
        }

        PaymentResult result = charge(request);
        String payload = objectMapper.writeValueAsString(result);

        outboxRepository.save(new OutboxEntity(
                null, event.getOrderId(), payload,
                OutboxStatus.NEW, Instant.now()));
        event.setStatus(InboxStatus.PROCESSED);
    }

    private PaymentResult charge(PaymentRequest request) {
        Optional<AccountsEntity> found =
                accountsRepository.findByUserIdForUpdate(request.userId());

        if (found.isEmpty()) {
            return new PaymentResult("CANCELED", "ACCOUNT_NOT_FOUND");
        }

        AccountsEntity entity = found.orElseThrow();
        Account account = accountDbMapper.toDomain(entity);
        if (!account.tryWithdraw(request.amount())) {
            return new PaymentResult("CANCELED", "INSUFFICIENT_FUNDS");
        }

        entity.setBalance(account.getBalance());
        return new PaymentResult("PAID", null);
    }

    public record PaymentResult(String status, String reason) {
    }

    public record PaymentRequest(
            UUID userId,
            BigDecimal amount) {
    }
}