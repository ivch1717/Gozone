package com.example.orders.db;

import com.example.orders.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderDbMapper {
    public OrderEntity toEntity(Order order){
        return new OrderEntity(order.getId(), order.getUserId(), order.getName(), order.getAmount(), order.getDescription(),
                order.getStatus());
    }

    public Order toDomain(OrderEntity entity){
        return new Order(entity.getId(), entity.getUserId(), entity.getName(), entity.getAmount(), entity.getStatus(),
                entity.getDescription());
    }
}
