package com.example.orders.application;

import com.example.orders.db.*;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrdersService {
    private final OrdersRepository ordersRepository;
    private final OutboxRepository outboxRepository;
    private final OrderDbMapper orderDbMapper;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(OrdersService.class);

    public OrdersService(OrdersRepository ordersRepository, OrderDbMapper mapper, OutboxRepository outboxRepository,
                         ObjectMapper objectMapper){
        this.ordersRepository = ordersRepository;
        this.orderDbMapper = mapper;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(
            UUID userId, String name, BigDecimal amount, String description) {

        OrderEntity entity = new OrderEntity(
                null, userId, name, amount, description, OrderStatus.NEW);

        OrderEntity saved = ordersRepository.save(entity);

        String payload = objectMapper.writeValueAsString(
                Map.of("userId", saved.getUserId(), "amount", saved.getAmount()));
        OutboxEntity event = new OutboxEntity(
                null, saved.getId(), payload, OutboxStatus.NEW, Instant.now());
        outboxRepository.save(event);

        return orderDbMapper.toDomain(saved);
    }

    public List<Order> getAllOrders(UUID userId) {
        List<Order> orders = ordersRepository.findAllByUserId(userId)
                .stream()
                .map(orderDbMapper::toDomain)
                .toList();

        log.debug("Получены заказы. userId={}, count={}", userId, orders.size());

        return orders;
    }

    public Order getOrderById(UUID id) {
        Order order = orderDbMapper.toDomain(
                ordersRepository.findById(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException("Заказ не найден")));

        log.debug("Получен заказ. orderId={}", id);

        return order;
    }
}
