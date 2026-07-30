package com.fadesp.paymentsapi.dto;

import com.fadesp.paymentsapi.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequestDTO {

    @NotNull(message = "Código do débito é obrigatório.")
    @Positive(message = "Código do débito deve ser um número positivo.")
    private Integer debtCode;

    @NotBlank(message = "CPF/CNPJ do pagador é obrigatório.")
    private String cpfCnpj;

    @NotNull(message = "Método de pagamento é obrigatório.")
    private PaymentMethod paymentMethod;

    private String cardNumber;

    @NotNull(message = "Valor do pagamento é obrigatório.")
    @Positive(message = "Valor do pagamento deve ser maior que zero.")
    private BigDecimal amount;
}
