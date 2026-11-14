package com.example.payments.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InboxProcessor {

    private final PaymentProcessingService paymentProcessingService;

    public InboxProcessor(
            PaymentProcessingService paymentProcessingService) {

        this.paymentProcessingService = paymentProcessingService;
    }

    @Scheduled(fixedDelay = 100)
    public void process() {
        paymentProcessingService.processNext();
    }
}