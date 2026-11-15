package com.example.orders.domain;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class OrderTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @Test void retainsOrderDetails() {
        Order order = new Order(ID, USER, "Keyboard", new BigDecimal("19.99"), OrderStatus.NEW, "description");
        assertThat(order).extracting(Order::getId, Order::getUserId, Order::getName,
                Order::getAmount, Order::getStatus, Order::getDescription)
                .containsExactly(ID, USER, "Keyboard", new BigDecimal("19.99"), OrderStatus.NEW, "description");
    }

    static Stream<Arguments> invalidOrders() {
        return Stream.of(
                Arguments.of(null, USER, "Order", BigDecimal.ONE, OrderStatus.NEW),
                Arguments.of(ID, null, "Order", BigDecimal.ONE, OrderStatus.NEW),
                Arguments.of(ID, USER, null, BigDecimal.ONE, OrderStatus.NEW),
                Arguments.of(ID, USER, "", BigDecimal.ONE, OrderStatus.NEW),
                Arguments.of(ID, USER, "  ", BigDecimal.ONE, OrderStatus.NEW),
                Arguments.of(ID, USER, "Order", null, OrderStatus.NEW),
                Arguments.of(ID, USER, "Order", BigDecimal.ZERO, OrderStatus.NEW),
                Arguments.of(ID, USER, "Order", BigDecimal.ONE.negate(), OrderStatus.NEW),
                Arguments.of(ID, USER, "Order", BigDecimal.ONE, null));
    }

    @ParameterizedTest @MethodSource("invalidOrders")
    void rejectsInvalidOrders(UUID id, UUID user, String name, BigDecimal amount, OrderStatus status) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(id, user, name, amount, status, null));
    }

    @ParameterizedTest @EnumSource(value = OrderStatus.class, names = {"PAID", "CANCELED"})
    void transitionsToFinalStatusAndAcceptsItsDuplicate(OrderStatus target) {
        Order order = new Order(ID, USER, "Order", BigDecimal.TEN, OrderStatus.NEW, null);
        if (target == OrderStatus.PAID) { order.markPaid(); order.markPaid(); }
        else { order.markCanceled(); order.markCanceled(); }
        assertThat(order.getStatus()).isEqualTo(target);
    }

    @Test void cannotCancelPaidOrder() {
        Order order = new Order(ID, USER, "Order", BigDecimal.TEN, OrderStatus.PAID, null);
        assertThatIllegalStateException().isThrownBy(order::markCanceled);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test void cannotPayCanceledOrder() {
        Order order = new Order(ID, USER, "Order", BigDecimal.TEN, OrderStatus.CANCELED, null);
        assertThatIllegalStateException().isThrownBy(order::markPaid);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }
}
