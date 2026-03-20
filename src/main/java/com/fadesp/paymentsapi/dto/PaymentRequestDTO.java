package com.fadesp.paymentsapi.dto;

import com.fadesp.paymentsapi.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequestDTO {

    private Integer debtCode;

    private String cpfCnpj;

    private PaymentMethod paymentMethod;

    private String cardNumber;

    private BigDecimal amount;
}
