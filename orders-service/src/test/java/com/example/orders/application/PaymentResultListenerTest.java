package com.example.orders.application;

import com.example.orders.domain.OrderStatus;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentResultListenerTest {
    private final OrdersService service = mock(OrdersService.class);
    private final PaymentResultListener listener = new PaymentResultListener(service, new ObjectMapper());
    private final UUID id = UUID.randomUUID();
    private ConsumerRecord<String, String> message(String key, String payload) {
        return new ConsumerRecord<>("payment-results", 0, 0, key, payload);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void appliesResultsAndAcceptsDuplicateNotifications(boolean changed) {
        when(service.applyPaymentResult(id, OrderStatus.PAID)).thenReturn(changed);
        listener.receive(message(id.toString(), "{\"status\":\"PAID\",\"reason\":null}"));
        verify(service).applyPaymentResult(id, OrderStatus.PAID);
    }

    @Test void passesCancellationToService() {
        listener.receive(message(id.toString(), "{\"status\":\"CANCELED\",\"reason\":\"INSUFFICIENT_FUNDS\"}"));
        verify(service).applyPaymentResult(id, OrderStatus.CANCELED);
    }

    @Test void rejectsMissingKeyOrPayload() {
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(message(null, "{}")));
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(message(id.toString(), null)));
        verifyNoInteractions(service);
    }

    @ParameterizedTest @ValueSource(strings = {"null", "{}", "{\"status\":null}", "{", "{\"status\":\"UNKNOWN\"}"})
    void rejectsBadResultsWithoutUpdatingOrder(String payload) {
        assertThatThrownBy(() -> listener.receive(message(id.toString(), payload))).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(service);
    }

    @Test void invalidIdentifierDoesNotReachService() {
        assertThatIllegalArgumentException().isThrownBy(() -> listener.receive(message("bad", "{\"status\":\"PAID\"}")));
        verifyNoInteractions(service);
    }

    @Test void serviceFailureEscapesForKafkaRetry() {
        RuntimeException failure = new RuntimeException("database unavailable");
        when(service.applyPaymentResult(id, OrderStatus.PAID)).thenThrow(failure);
        assertThatThrownBy(() -> listener.receive(message(id.toString(), "{\"status\":\"PAID\"}"))).isSameAs(failure);
    }
}
