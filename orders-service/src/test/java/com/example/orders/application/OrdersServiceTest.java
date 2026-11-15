package com.example.orders.application;

import com.example.orders.db.*;
import com.example.orders.domain.*;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdersServiceTest {
    private final OrdersRepository orders = mock(OrdersRepository.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final ObjectMapper json = new ObjectMapper();
    private final UUID id = UUID.randomUUID(), user = UUID.randomUUID();
    private OrdersService service;

    @BeforeEach void setUp() { service = new OrdersService(orders, new OrderDbMapper(), outbox, json); }
    private OrderEntity entity(OrderStatus status) {
        return new OrderEntity(id, user, "Order", new BigDecimal("25.50"), "Description", status);
    }

    @Test void createsNewOrderAndPaymentEventWithMatchingDetails() {
        when(orders.save(any())).thenAnswer(call -> {
            OrderEntity entity = call.getArgument(0);
            assertThat(entity.getId()).isNull();
            entity.setId(id);
            return entity;
        });
        Order result = service.createOrder(user, "Order", new BigDecimal("25.50"), "Description");
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.NEW);
        var captor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outbox).save(captor.capture());
        OutboxEntity event = captor.getValue();
        assertThat(event.getId()).isNull();
        assertThat(event.getOrderId()).isEqualTo(id);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
        assertThat(event.getCreatedAt()).isNotNull();
        var payload = json.readTree(event.getPayload());
        assertThat(payload.get("userId").asText()).isEqualTo(user.toString());
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("25.50");
    }

    @Test void doesNotPublishWhenOrderCannotBeSaved() {
        RuntimeException failure = new RuntimeException("database unavailable");
        when(orders.save(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.createOrder(user, "Order", BigDecimal.ONE, null)).isSameAs(failure);
        verifyNoInteractions(outbox);
    }

    @ParameterizedTest @EnumSource(value = OrderStatus.class, names = {"PAID", "CANCELED"})
    void appliesFinalPaymentResult(OrderStatus status) {
        OrderEntity entity = entity(OrderStatus.NEW);
        when(orders.findByIdForUpdate(id)).thenReturn(Optional.of(entity));
        assertThat(service.applyPaymentResult(id, status)).isTrue();
        assertThat(entity.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest @EnumSource(value = OrderStatus.class, names = {"PAID", "CANCELED"})
    void doesNotOverwriteAlreadyFinalOrder(OrderStatus status) {
        OrderEntity entity = entity(status);
        when(orders.findByIdForUpdate(id)).thenReturn(Optional.of(entity));
        assertThat(service.applyPaymentResult(id, OrderStatus.PAID)).isFalse();
        assertThat(entity.getStatus()).isEqualTo(status);
    }

    @Test void rejectsMissingOrderAndInvalidPaymentStatus() {
        when(orders.findByIdForUpdate(id)).thenReturn(Optional.empty());
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> service.applyPaymentResult(id, OrderStatus.PAID));
        assertThatIllegalArgumentException().isThrownBy(() -> service.applyPaymentResult(id, OrderStatus.NEW));
        assertThatIllegalArgumentException().isThrownBy(() -> service.applyPaymentResult(id, null));
    }

    @Test void mapsPageWithoutLosingMetadataOrUserFilter() {
        Pageable pageable = PageRequest.of(2, 5, Sort.by("amount"));
        when(orders.findAllByUserId(user, pageable)).thenReturn(new PageImpl<>(List.of(entity(OrderStatus.PAID)), pageable, 16));
        Page<Order> page = service.getAllOrders(user, pageable);
        assertThat(page.getTotalElements()).isEqualTo(16);
        assertThat(page.getNumber()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.getContent()).singleElement().satisfies(order -> {
            assertThat(order.getId()).isEqualTo(id);
            assertThat(order.getUserId()).isEqualTo(user);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        });
    }

    @Test void returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orders.findAllByUserId(user, pageable)).thenReturn(Page.empty(pageable));
        assertThat(service.getAllOrders(user, pageable)).isEmpty();
    }

    @Test void findsOrderOrReportsAbsence() {
        when(orders.findById(id)).thenReturn(Optional.of(entity(OrderStatus.NEW)), Optional.empty());
        assertThat(service.getOrderById(id).getId()).isEqualTo(id);
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> service.getOrderById(id));
    }
}
