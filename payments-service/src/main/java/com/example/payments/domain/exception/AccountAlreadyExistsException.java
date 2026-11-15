package com.example.payments.domain.exception;

import java.util.UUID;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException(UUID userId) {
        super("Счёт пользователя " + userId + " уже существует");
    }

    public AccountAlreadyExistsException(UUID userId, Throwable cause) {
        super("Счёт пользователя " + userId + " уже существует", cause);
    }
}
