package com.example.payments.db;

import com.example.payments.domain.Account;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AccountDbMapperTest {
    @Test void preservesIdentityAndExactBalanceInRoundTrip() {
        var original = new Account(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("123.45"));
        var mapper = new AccountDbMapper();
        assertThat(mapper.toDomain(mapper.toEntity(original))).usingRecursiveComparison().isEqualTo(original);
    }
}
