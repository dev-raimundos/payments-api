package com.fadesp.paymentsapi.dto;

import com.fadesp.paymentsapi.enums.PaymentMethod;
import com.fadesp.paymentsapi.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentResponseDTO {

    private Long id;

    private Integer debtCode;

    private String cpfCnpj;

    private PaymentMethod paymentMethod;

    private String cardNumber;

    private BigDecimal amount;

    private PaymentStatus status;
}
