package com.example.orders.domain;

import com.example.orders.db.OrderDbMapper;
import com.example.orders.db.OrderEntity;
import com.example.orders.db.OrdersRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrdersService {
    private final OrdersRepository ordersRepository;
    private final OrderDbMapper orderDbMapper;

    public OrdersService(OrdersRepository ordersRepository, OrderDbMapper mapper){
        this.ordersRepository = ordersRepository;
        this.orderDbMapper = mapper;
    }

    public Order createOrder(UUID userId, String name, BigDecimal amount, String description){
        OrderEntity entity = new OrderEntity(null, userId, name, amount, description, OrderStatus.NEW);
        OrderEntity saved = ordersRepository.save(entity);

        return orderDbMapper.toDomain(saved);
    }

    public List<Order> getAllOrders(UUID userId) {
        return ordersRepository.findAllByUserId(userId)
                .stream()
                .map(orderDbMapper::toDomain)
                .toList();
    }

    public Order getOrderById(UUID id){
        return orderDbMapper.toDomain(ordersRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException("Заказ не найден")));
    }
}
