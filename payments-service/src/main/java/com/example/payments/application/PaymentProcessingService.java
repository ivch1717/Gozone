package com.example.payments.application;

import com.example.payments.application.exception.InvalidPaymentRequestException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

@Service
public class PaymentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);

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

        PaymentRequest request;
        try {
            request = parseAndValidate(event.getPayload());
        } catch (InvalidPaymentRequestException ex) {
            saveResult(event.getOrderId(),
                    new PaymentResult("CANCELED", "INVALID_PAYMENT_REQUEST"));
            event.markFailed(ex.getMessage());
            log.warn("Некорректная заявка отклонена. eventId={}, orderId={}, reason={}",
                    event.getEventId(), event.getOrderId(), ex.getMessage());
            return;
        }

        PaymentResult result = charge(request);
        saveResult(event.getOrderId(), result);
        event.setStatus(InboxStatus.PROCESSED);
    }

    private PaymentRequest parseAndValidate(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidPaymentRequestException("Пустая заявка на оплату");
        }

        PaymentRequest request;
        try {
            request = objectMapper.readerFor(PaymentRequest.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(payload);
        } catch (StreamReadException | MismatchedInputException ex) {
            throw new InvalidPaymentRequestException(
                    "Некорректный JSON или типы полей заявки на оплату", ex);
        }

        if (request == null || request.userId() == null) {
            throw new InvalidPaymentRequestException("В заявке на оплату отсутствует userId");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new InvalidPaymentRequestException("Сумма оплаты должна быть положительной");
        }
        return request;
    }

    private void saveResult(UUID orderId, PaymentResult result) {
        String payload = objectMapper.writeValueAsString(result);
        outboxRepository.save(new OutboxEntity(
                null, orderId, payload, OutboxStatus.NEW, Instant.now()));
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
