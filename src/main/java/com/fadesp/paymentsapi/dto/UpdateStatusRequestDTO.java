package com.fadesp.paymentsapi.dto;

import com.fadesp.paymentsapi.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequestDTO {

    @NotNull(message = "Novo status é obrigatório.")
    private PaymentStatus status;
}
