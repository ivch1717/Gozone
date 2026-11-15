package com.example.payments.application;

import com.example.payments.db.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class OutboxPublisherTest {
    private final OutboxRepository repository = mock(OutboxRepository.class);
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final OutboxPublisher publisher = new OutboxPublisher(repository, kafka);
    private final OutboxEntity event = new OutboxEntity(UUID.randomUUID(), UUID.randomUUID(),
            "{\"amount\":10}", OutboxStatus.NEW, Instant.now());

    @Test void emptyOutboxDoesNotSendAnything() {
        when(repository.findFirstByStatusOrderByCreatedAtAsc(OutboxStatus.NEW)).thenReturn(Optional.empty());
        publisher.publish();
        verifyNoInteractions(kafka);
        verify(repository, never()).save(any());
    }

    @Test void sendsStableIdentityAndMarksSentOnlyAfterAcknowledgement() throws Exception {
        when(repository.findFirstByStatusOrderByCreatedAtAsc(OutboxStatus.NEW)).thenReturn(Optional.of(event));
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafka.send(any(ProducerRecord.class))).thenReturn(future);
        when(future.get(10, TimeUnit.SECONDS)).thenAnswer(call -> {
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
            verify(repository, never()).save(any());
            return null;
        });
        publisher.publish();
        var message = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(message.capture());
        assertThat(message.getValue().topic()).isEqualTo("payment-results");
        assertThat(message.getValue().key()).isEqualTo(event.getOrderId().toString());
        assertThat(message.getValue().value()).isEqualTo(event.getPayload());
        assertThat(new String(message.getValue().headers().lastHeader("eventId").value(), StandardCharsets.UTF_8))
                .isEqualTo(event.getId().toString());
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(repository).save(event);
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void leavesEventPendingWhenDeliveryFails(int failureType) throws Exception {
        when(repository.findFirstByStatusOrderByCreatedAtAsc(OutboxStatus.NEW)).thenReturn(Optional.of(event));
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafka.send(any(ProducerRecord.class))).thenReturn(future);
        Exception failure = switch (failureType) {
            case 0 -> new TimeoutException("Kafka timeout");
            case 1 -> new ExecutionException(new RuntimeException("broker unavailable"));
            default -> new IllegalStateException("producer closed");
        };
        when(future.get(10, TimeUnit.SECONDS)).thenThrow(failure);
        publisher.publish();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
        verify(repository, never()).save(any());
    }

    @Test void preservesThreadInterruptionAndDoesNotMarkSent() throws Exception {
        when(repository.findFirstByStatusOrderByCreatedAtAsc(OutboxStatus.NEW)).thenReturn(Optional.of(event));
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafka.send(any(ProducerRecord.class))).thenReturn(future);
        when(future.get(10, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
        try {
            publisher.publish();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
            verify(repository, never()).save(any());
        } finally { Thread.interrupted(); }
    }
}

