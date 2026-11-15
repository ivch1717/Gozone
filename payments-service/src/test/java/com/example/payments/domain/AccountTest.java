package com.example.payments.domain;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class AccountTest {
    private final UUID id = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();

    @Test void retainsIdentityAndAllowsZeroBalance() {
        Account account = new Account(id, user, BigDecimal.ZERO);
        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getUserId()).isEqualTo(user);
        assertThat(account.getBalance()).isZero();
    }

    @Test void rejectsMissingIdentityAndNegativeBalance() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Account(null, user, BigDecimal.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new Account(id, null, BigDecimal.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new Account(id, user, BigDecimal.ONE.negate()));
    }

    @Test void addsMoneyExactly() {
        Account account = new Account(id, user, new BigDecimal("0.10"));
        account.topUp(new BigDecimal("0.20"));
        assertThat(account.getBalance()).isEqualByComparingTo("0.30");
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"0", "-0.01"})
    void rejectsInvalidTopUpsWithoutChangingBalance(String amount) {
        Account account = new Account(id, user, BigDecimal.TEN);
        assertThatIllegalArgumentException().isThrownBy(() -> account.topUp(amount == null ? null : new BigDecimal(amount)));
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"0", "-1"})
    void rejectsInvalidWithdrawalsWithoutChangingBalance(String amount) {
        Account account = new Account(id, user, BigDecimal.TEN);
        assertThatIllegalArgumentException().isThrownBy(() -> account.tryWithdraw(amount == null ? null : new BigDecimal(amount)));
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test void withdrawsOnlyWhenFundsAreSufficient() {
        Account account = new Account(id, user, new BigDecimal("10.50"));
        assertThat(account.tryWithdraw(new BigDecimal("11"))).isFalse();
        assertThat(account.getBalance()).isEqualByComparingTo("10.50");
        assertThat(account.tryWithdraw(new BigDecimal("0.50"))).isTrue();
        assertThat(account.getBalance()).isEqualByComparingTo("10.00");
        assertThat(account.tryWithdraw(BigDecimal.TEN)).isTrue();
        assertThat(account.getBalance()).isZero();
    }
}
