package com.example.orders.application;

import com.example.orders.db.OutboxEntity;
import com.example.orders.db.OutboxRepository;
import com.example.orders.db.OutboxStatus;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        var pending = outboxRepository
                .findFirstByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);

        if (pending.isEmpty()) {
            return;
        }

        OutboxEntity event = pending.orElseThrow();

        try {
            var message = new ProducerRecord<>(
                    "payment-requests",
                    event.getOrderId().toString(),
                    event.getPayload());

            message.headers().add(
                    "eventId",
                    event.getId().toString().getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(message).get(10, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.SENT);
            outboxRepository.save(event);

            log.info("Событие отправлено. eventId={}", event.getId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException | RuntimeException ex) {
            log.warn("Ошибка отправки события. eventId={}", event.getId(), ex);
        }
    }
}