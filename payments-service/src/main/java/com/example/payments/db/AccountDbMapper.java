package com.example.payments.db;

import com.example.payments.domain.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountDbMapper {
    public AccountsEntity toEntity(Account account){
        return new AccountsEntity(account.getId(), account.getUserId(), account.getBalance());
    }

    public Account toDomain(AccountsEntity entity){
        return new Account(entity.getId(), entity.getUserId(), entity.getBalance());
    }
}
