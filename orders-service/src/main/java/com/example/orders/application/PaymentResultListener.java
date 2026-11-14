package com.example.orders.application;

import com.example.orders.domain.OrderStatus;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentResultListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);
    private final OrdersService ordersService;
    private final ObjectMapper objectMapper;

    public PaymentResultListener(OrdersService ordersService, ObjectMapper objectMapper) {
        this.ordersService = ordersService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-results")
    public void receive(ConsumerRecord<String, String> message) {
        if (message.key() == null || message.value() == null) {
            throw new IllegalArgumentException("В результате оплаты отсутствует orderId или payload");
        }
        UUID orderId = UUID.fromString(message.key());
        PaymentResult result = objectMapper.readValue(message.value(), PaymentResult.class);
        if (result == null || result.status() == null) {
            throw new IllegalArgumentException("В результате оплаты отсутствует статус");
        }

        boolean changed = ordersService.applyPaymentResult(orderId, result.status());
        if (changed) {
            log.info("Статус заказа изменён. orderId={}, status={}, reason={}",
                    orderId, result.status(), result.reason());
        } else {
            log.debug("Заказ уже обработан. orderId={}", orderId);
        }
    }

    public record PaymentResult(OrderStatus status, String reason) {
    }
}
