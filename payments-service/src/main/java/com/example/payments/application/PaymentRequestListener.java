package com.example.payments.application;

import com.example.payments.db.InboxRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestListener {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentRequestListener.class);

    private final InboxRepository inboxRepository;

    public PaymentRequestListener(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @KafkaListener(topics = "payment-requests")
    public void receive(ConsumerRecord<String, String> message) {
        Header eventIdHeader = message.headers().lastHeader("eventId");

        if (eventIdHeader == null || eventIdHeader.value() == null) {
            throw new IllegalArgumentException(
                    "В сообщении отсутствует заголовок eventId");
        }

        if (message.key() == null || message.value() == null) {
            throw new IllegalArgumentException(
                    "В сообщении отсутствует orderId или payload");
        }

        UUID eventId = UUID.fromString(
                new String(eventIdHeader.value(), StandardCharsets.UTF_8));

        UUID orderId = UUID.fromString(message.key());
        String payload = message.value();

        int inserted = inboxRepository.insertIfAbsent(
                eventId,
                orderId,
                payload,
                Instant.now());

        if (inserted == 1) {
            log.info(
                    "Заявка сохранена в inbox. eventId={}, orderId={}",
                    eventId,
                    orderId);
        } else {
            log.debug(
                    "Повторное событие уже есть в inbox. eventId={}",
                    eventId);
        }
    }
}