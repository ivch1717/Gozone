package com.example.payments.application;

import com.example.payments.db.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentProcessingServiceTest {
    private final InboxRepository inbox = mock(InboxRepository.class);
    private final AccountsRepository accounts = mock(AccountsRepository.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final ObjectMapper json = new ObjectMapper();
    private final PaymentProcessingService service = new PaymentProcessingService(
            inbox, accounts, new AccountDbMapper(), json, outbox);
    private final UUID user = UUID.randomUUID(), orderId = UUID.randomUUID();

    private InboxEntity pending(String payload) {
        var event = new InboxEntity(UUID.randomUUID(), orderId, payload, InboxStatus.NEW, Instant.now());
        when(inbox.findNextForProcessing()).thenReturn(Optional.of(event));
        return event;
    }
    private String validPayload() { return "{\"userId\":\"" + user + "\",\"amount\":25.50}"; }
    private void assertResult(String status, String reason) {
        var captor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outbox).save(captor.capture());
        var event = captor.getValue();
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
        assertThat(event.getCreatedAt()).isNotNull();
        var result = json.readValue(event.getPayload(), PaymentProcessingService.PaymentResult.class);
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.reason()).isEqualTo(reason);
    }

    @Test void emptyInboxDoesNothing() {
        when(inbox.findNextForProcessing()).thenReturn(Optional.empty());
        service.processNext();
        verifyNoInteractions(accounts, outbox);
    }

    @ParameterizedTest @NullSource
    @ValueSource(strings = {"", "  ", "{", "null", "[]", "{}", "{\"userId\":\"invalid\",\"amount\":1}",
            "{\"userId\":null,\"amount\":1}", "{\"userId\":\"11111111-1111-1111-1111-111111111111\"}",
            "{\"userId\":\"11111111-1111-1111-1111-111111111111\",\"amount\":0}",
            "{\"userId\":\"11111111-1111-1111-1111-111111111111\",\"amount\":-1}",
            "{\"userId\":\"11111111-1111-1111-1111-111111111111\",\"amount\":\"bad\"}",
            "{\"userId\":\"11111111-1111-1111-1111-111111111111\",\"amount\":1} {}"})
    void rejectsBadDataWithoutTouchingAccountsAndPublishesCancellation(String payload) {
        InboxEntity event = pending(payload);
        service.processNext();
        assertThat(event.getStatus()).isEqualTo(InboxStatus.FAILED);
        assertThat(event.getFailureReason()).isNotBlank();
        verifyNoInteractions(accounts);
        assertResult("CANCELED", "INVALID_PAYMENT_REQUEST");
    }

    @Test void paysAndMarksInboxProcessed() {
        InboxEntity event = pending(validPayload());
        AccountsEntity account = new AccountsEntity(UUID.randomUUID(), user, new BigDecimal("100.00"));
        when(accounts.findByUserIdForUpdate(user)).thenReturn(Optional.of(account));
        service.processNext();
        assertThat(account.getBalance()).isEqualByComparingTo("74.50");
        assertThat(event.getStatus()).isEqualTo(InboxStatus.PROCESSED);
        assertThat(event.getFailureReason()).isNull();
        assertResult("PAID", null);
    }

    @Test void refusesInsufficientFundsWithoutDebiting() {
        InboxEntity event = pending(validPayload());
        AccountsEntity account = new AccountsEntity(UUID.randomUUID(), user, BigDecimal.TEN);
        when(accounts.findByUserIdForUpdate(user)).thenReturn(Optional.of(account));
        service.processNext();
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(event.getStatus()).isEqualTo(InboxStatus.PROCESSED);
        assertResult("CANCELED", "INSUFFICIENT_FUNDS");
    }

    @Test void refusesMissingAccount() {
        InboxEntity event = pending(validPayload());
        when(accounts.findByUserIdForUpdate(user)).thenReturn(Optional.empty());
        service.processNext();
        assertThat(event.getStatus()).isEqualTo(InboxStatus.PROCESSED);
        assertResult("CANCELED", "ACCOUNT_NOT_FOUND");
    }

    @Test void propagatesDatabaseFailureInsteadOfMarkingDataInvalid() {
        InboxEntity event = pending(validPayload());
        RuntimeException failure = new RuntimeException("database unavailable");
        when(accounts.findByUserIdForUpdate(user)).thenThrow(failure);
        assertThatThrownBy(service::processNext).isSameAs(failure);
        assertThat(event.getStatus()).isEqualTo(InboxStatus.NEW);
        verifyNoInteractions(outbox);
    }

    @Test void failureToSaveCancellationIsRetriedNotMarkedFailed() {
        InboxEntity event = pending("{");
        RuntimeException failure = new RuntimeException("outbox unavailable");
        when(outbox.save(any())).thenThrow(failure);
        assertThatThrownBy(service::processNext).isSameAs(failure);
        assertThat(event.getStatus()).isEqualTo(InboxStatus.NEW);
        verifyNoInteractions(accounts);
    }

    @Test void outboxFailurePropagatesForTransactionRollback() {
        InboxEntity event = pending(validPayload());
        when(accounts.findByUserIdForUpdate(user)).thenReturn(Optional.of(
                new AccountsEntity(UUID.randomUUID(), user, BigDecimal.valueOf(100))));
        RuntimeException failure = new RuntimeException("outbox unavailable");
        when(outbox.save(any())).thenThrow(failure);
        assertThatThrownBy(service::processNext).isSameAs(failure);
        assertThat(event.getStatus()).isEqualTo(InboxStatus.NEW);
    }

    @Test void schedulerDelegatesToTransactionalService() {
        PaymentProcessingService processor = mock(PaymentProcessingService.class);
        new InboxProcessor(processor).process();
        verify(processor).processNext();
    }
}
