package com.example.payments.application;

import com.example.payments.db.InboxRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentRequestListenerTest {
    private final InboxRepository inbox = mock(InboxRepository.class);
    private final PaymentRequestListener listener = new PaymentRequestListener(inbox);
    private final UUID eventId = UUID.randomUUID(), orderId = UUID.randomUUID();
    private ConsumerRecord<String, String> message(String key, String payload, String header) {
        var record = new ConsumerRecord<String, String>("payment-requests", 0, 0, key, payload);
        record.headers().add("eventId", header == null ? null : header.getBytes(StandardCharsets.UTF_8));
        return record;
    }

    @ParameterizedTest @ValueSource(ints = {0, 1})
    void recordsNewEventsAndAcceptsDuplicates(int inserted) {
        when(inbox.insertIfAbsent(eq(eventId), eq(orderId), eq("{}"), any())).thenReturn(inserted);
        listener.receive(message(orderId.toString(), "{}", eventId.toString()));
        verify(inbox).insertIfAbsent(eq(eventId), eq(orderId), eq("{}"), any());
    }

    @Test void rejectsMissingHeader() {
        var record = new ConsumerRecord<String, String>("payment-requests", 0, 0, orderId.toString(), "{}");
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(record));
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(message(orderId.toString(), "{}", null)));
        verifyNoInteractions(inbox);
    }

    @Test void rejectsMissingKeyOrPayload() {
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(message(null, "{}", eventId.toString())));
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(message(orderId.toString(), null, eventId.toString())));
        verifyNoInteractions(inbox);
    }

    @Test void rejectsInvalidIdentifiers() {
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(message("bad", "{}", eventId.toString())));
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(message(orderId.toString(), "{}", "bad")));
        verifyNoInteractions(inbox);
    }

    @Test void databaseFailureEscapesSoKafkaCanRetry() {
        RuntimeException failure = new RuntimeException("database unavailable");
        when(inbox.insertIfAbsent(any(), any(), any(), any())).thenThrow(failure);
        assertThatThrownBy(() -> listener.receive(message(orderId.toString(), "{}", eventId.toString()))).isSameAs(failure);
    }
}
