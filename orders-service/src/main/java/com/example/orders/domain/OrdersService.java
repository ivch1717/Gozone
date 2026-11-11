package com.example.orders.domain;

import com.example.orders.db.OrderDbMapper;
import com.example.orders.db.OrderEntity;
import com.example.orders.db.OrdersRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrdersService {
    private final OrdersRepository ordersRepository;
    private final OrderDbMapper orderDbMapper;
    private static final Logger log = LoggerFactory.getLogger(OrdersService.class);

    public OrdersService(OrdersRepository ordersRepository, OrderDbMapper mapper){
        this.ordersRepository = ordersRepository;
        this.orderDbMapper = mapper;
    }

    public Order createOrder(UUID userId, String name, BigDecimal amount, String description){
        OrderEntity entity = new OrderEntity(null, userId, name, amount, description, OrderStatus.NEW);
        OrderEntity saved = ordersRepository.save(entity);
        log.info("Заказ создан. orderId={}", saved.getId());

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
