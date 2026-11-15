package com.example.payments.application;

import com.example.payments.db.*;
import com.example.payments.domain.exception.AccountAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentsServiceTest {
    private final AccountsRepository repository = mock(AccountsRepository.class);
    private final PaymentsService service = new PaymentsService(repository, new AccountDbMapper());
    private final UUID id = UUID.randomUUID(), user = UUID.randomUUID();

    @Test void createsZeroBalanceAccountWithGeneratedId() {
        when(repository.save(any())).thenAnswer(call -> {
            AccountsEntity entity = call.getArgument(0);
            assertThat(entity.getId()).isNull();
            assertThat(entity.getUserId()).isEqualTo(user);
            assertThat(entity.getBalance()).isZero();
            entity.setId(id);
            return entity;
        });
        var account = service.createAccount(user);
        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getUserId()).isEqualTo(user);
        assertThat(account.getBalance()).isZero();
    }

    @Test void rejectsExistingAccountBeforeSave() {
        when(repository.existsByUserId(user)).thenReturn(true);
        assertThatExceptionOfType(AccountAlreadyExistsException.class).isThrownBy(() -> service.createAccount(user));
        verify(repository, never()).save(any());
    }

    @Test void translatesConcurrentDuplicateWhileRetainingCause() {
        var failure = new DataIntegrityViolationException("unique user_id");
        when(repository.existsByUserId(user)).thenReturn(false, true);
        when(repository.save(any())).thenThrow(failure);
        assertThatExceptionOfType(AccountAlreadyExistsException.class)
                .isThrownBy(() -> service.createAccount(user)).withCause(failure);
    }

    @Test void doesNotHideUnrelatedDatabaseConstraintFailure() {
        var failure = new DataIntegrityViolationException("another constraint");
        when(repository.save(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.createAccount(user)).isSameAs(failure);
    }

    @Test void readsBalanceOrReportsMissingAccount() {
        when(repository.findByUserId(user)).thenReturn(
                Optional.of(new AccountsEntity(id, user, new BigDecimal("12.34"))), Optional.empty());
        assertThat(service.getBalance(user)).isEqualByComparingTo("12.34");
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> service.getBalance(user));
    }

    @Test void topsUpLockedEntityAndReturnsNewBalance() {
        AccountsEntity entity = new AccountsEntity(id, user, new BigDecimal("10.10"));
        when(repository.findByUserIdForUpdate(user)).thenReturn(Optional.of(entity));
        var account = service.topUpAccount(user, new BigDecimal("0.20"));
        assertThat(account.getBalance()).isEqualByComparingTo("10.30");
        assertThat(entity.getBalance()).isEqualByComparingTo("10.30");
        verify(repository).findByUserIdForUpdate(user);
    }

    @Test void rejectsInvalidTopUpWithoutChangingEntity() {
        AccountsEntity entity = new AccountsEntity(id, user, BigDecimal.TEN);
        when(repository.findByUserIdForUpdate(user)).thenReturn(Optional.of(entity));
        assertThatIllegalArgumentException().isThrownBy(() -> service.topUpAccount(user, BigDecimal.ZERO));
        assertThat(entity.getBalance()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test void cannotTopUpMissingAccount() {
        when(repository.findByUserIdForUpdate(user)).thenReturn(Optional.empty());
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> service.topUpAccount(user, BigDecimal.ONE));
    }
}
