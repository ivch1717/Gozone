package com.example.orders.api;

import com.example.orders.api.dtos.CreateOrderRequest;
import com.example.orders.api.dtos.OrderResponse;
import com.example.orders.application.OrdersService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrdersController {
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "amount", "status");
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

    @GetMapping("/users/{userId}")
    public ResponseEntity<PagedModel<OrderResponse>> getAllOrders(
            @PathVariable("userId") UUID userId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Недопустимое поле сортировки: " + order.getProperty());
            }
        }

        var orders = ordersService.getAllOrders(userId, pageable)
                .map(mapper::toOrderResponse);
        return ResponseEntity.ok(new PagedModel<>(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable("id") UUID id){
        return ResponseEntity.ok(mapper.toOrderResponse(ordersService.getOrderById(id)));
    }
}
