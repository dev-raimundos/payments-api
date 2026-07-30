package com.fadesp.paymentsapi.dto;

import com.fadesp.paymentsapi.enums.PaymentMethod;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private PaymentRequestDTO validRequest() {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setDebtCode(1001);
        dto.setCpfCnpj("12345678900");
        dto.setPaymentMethod(PaymentMethod.pix);
        dto.setAmount(new BigDecimal("150.00"));
        return dto;
    }

    @Test
    @DisplayName("Não deve haver violações para um pagamento válido")
    void shouldHaveNoViolationsForValidRequest() {
        Set<ConstraintViolation<PaymentRequestDTO>> violations = validator.validate(validRequest());
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar valor de pagamento negativo")
    void shouldRejectNegativeAmount() {
        PaymentRequestDTO dto = validRequest();
        dto.setAmount(new BigDecimal("-150.00"));

        Set<ConstraintViolation<PaymentRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("Deve rejeitar valor de pagamento igual a zero")
    void shouldRejectZeroAmount() {
        PaymentRequestDTO dto = validRequest();
        dto.setAmount(BigDecimal.ZERO);

        Set<ConstraintViolation<PaymentRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("Deve rejeitar código de débito negativo")
    void shouldRejectNegativeDebtCode() {
        PaymentRequestDTO dto = validRequest();
        dto.setDebtCode(-1001);

        Set<ConstraintViolation<PaymentRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("debtCode"));
    }

    @Test
    @DisplayName("Deve rejeitar código de débito nulo")
    void shouldRejectNullDebtCode() {
        PaymentRequestDTO dto = validRequest();
        dto.setDebtCode(null);

        Set<ConstraintViolation<PaymentRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("debtCode"));
    }

    @Test
    @DisplayName("Deve rejeitar CPF/CNPJ em branco")
    void shouldRejectBlankCpfCnpj() {
        PaymentRequestDTO dto = validRequest();
        dto.setCpfCnpj(" ");

        Set<ConstraintViolation<PaymentRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cpfCnpj"));
    }

    @Test
    @DisplayName("Deve rejeitar método de pagamento nulo")
    void shouldRejectNullPaymentMethod() {
        PaymentRequestDTO dto = validRequest();
        dto.setPaymentMethod(null);

        Set<ConstraintViolation<PaymentRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("paymentMethod"));
    }
}
