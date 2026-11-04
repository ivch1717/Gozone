package com.example.orders.api;

import com.example.orders.api.dtos.OrderResponse;
import com.example.orders.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderApiMapper {
    public OrderResponse toOrderResponse(Order order){
        return new OrderResponse(order.getId(), order.getUserId(), order.getName(), order.getAmount(),
                order.getDescription(), order.getStatus());
    }
}
