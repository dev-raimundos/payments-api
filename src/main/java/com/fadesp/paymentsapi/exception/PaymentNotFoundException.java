package com.fadesp.paymentsapi.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long id) {
        super("Pagemento não encontrado. ID: " + id);
    }
}
