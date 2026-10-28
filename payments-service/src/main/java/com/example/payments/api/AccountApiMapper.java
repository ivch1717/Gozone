package com.example.payments.api;

import com.example.payments.api.dtos.AccountResponse;
import com.example.payments.domain.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountApiMapper {
    public AccountResponse toAccountResponse(Account entity){
        return new AccountResponse(entity.getId(), entity.getUserId(), entity.getBalance());
    }
}
