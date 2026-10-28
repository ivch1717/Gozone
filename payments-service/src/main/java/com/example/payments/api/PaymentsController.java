package com.example.payments.api;

import com.example.payments.api.dtos.AccountResponse;
import com.example.payments.api.dtos.CreateAccountRequest;
import com.example.payments.domain.PaymentsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentsController {
    private static final Logger logger =
            LoggerFactory.getLogger(PaymentsController.class);

    private final PaymentsService paymentsService;
    private final AccountApiMapper accountApiMapper;

    public PaymentsController(PaymentsService paymentsService, AccountApiMapper accountApiMapper){

        this.paymentsService = paymentsService;
        this.accountApiMapper = accountApiMapper;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ){
        var response = paymentsService.createAccount(request.userId());

        return ResponseEntity.ok(accountApiMapper.toAccountResponse(response));
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalanceByUserId(
            @RequestParam("userId") UUID userId) {

        return ResponseEntity.ok(paymentsService.getBalance(userId));
    }
}
