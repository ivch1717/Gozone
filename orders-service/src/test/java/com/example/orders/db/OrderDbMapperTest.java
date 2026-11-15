package com.example.orders.db;

import com.example.orders.domain.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OrderDbMapperTest {
    @Test void preservesAllFieldsInRoundTrip() {
        var original = new Order(UUID.randomUUID(), UUID.randomUUID(), "Keyboard", new BigDecimal("49.99"), OrderStatus.PAID, "Description");
        var mapper = new OrderDbMapper();
        assertThat(mapper.toDomain(mapper.toEntity(original))).usingRecursiveComparison().isEqualTo(original);
    }
}
