package com.example.orders.api;

import com.example.orders.api.dtos.CreateOrderRequest;
import com.example.orders.api.dtos.OrderResponse;
import com.example.orders.domain.OrdersService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrdersController {
    private static final Logger logger =
            LoggerFactory.getLogger(OrdersController.class);

    private final OrdersService ordersService;
    private final OrderApiMapper mapper;

    public OrdersController(OrdersService ordersService, OrderApiMapper mapper){
        this.ordersService = ordersService;
        this.mapper = mapper;
    }

    @PostMapping()
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
            ){
        return ResponseEntity.ok(mapper.toOrderResponse(ordersService.createOrder(request.userId(), request.name(),
                request.amount(), request.description())));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getAllOrders( @PathVariable("userId") UUID userId){
        return ResponseEntity.ok(ordersService.getAllOrders(userId).stream().map(mapper::toOrderResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable("id") UUID id){
        return ResponseEntity.ok(mapper.toOrderResponse(ordersService.getOrderById(id)));
    }
}
