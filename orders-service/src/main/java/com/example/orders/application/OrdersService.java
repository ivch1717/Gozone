package com.example.orders.application;

import com.example.orders.db.*;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Transactional
    public boolean applyPaymentResult(UUID orderId, OrderStatus status) {
        if (status != OrderStatus.PAID && status != OrderStatus.CANCELED) {
            throw new IllegalArgumentException("Некорректный результат оплаты: " + status);
        }

        OrderEntity entity = ordersRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден: " + orderId));
        if (entity.getStatus() != OrderStatus.NEW) {
            return false;
        }

        Order order = orderDbMapper.toDomain(entity);
        if (status == OrderStatus.PAID) {
            order.markPaid();
        } else {
            order.markCanceled();
        }
        entity.setStatus(order.getStatus());
        return true;
    }

    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(UUID userId, Pageable pageable) {
        Page<Order> orders = ordersRepository.findAllByUserId(userId, pageable)
                .map(orderDbMapper::toDomain);

        log.debug("Получена страница заказов. userId={}, page={}, count={}, total={}",
                userId, orders.getNumber(), orders.getNumberOfElements(), orders.getTotalElements());

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
