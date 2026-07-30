package com.fadesp.paymentsapi.service;

import com.fadesp.paymentsapi.dto.PaymentRequestDTO;
import com.fadesp.paymentsapi.dto.PaymentResponseDTO;
import com.fadesp.paymentsapi.enums.PaymentMethod;
import com.fadesp.paymentsapi.enums.PaymentStatus;
import com.fadesp.paymentsapi.exception.InvalidStatusTransitionException;
import com.fadesp.paymentsapi.exception.PaymentNotFoundException;
import com.fadesp.paymentsapi.exception.PaymentValidationException;
import com.fadesp.paymentsapi.model.Payment;
import com.fadesp.paymentsapi.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment pendingPayment;
    private PaymentRequestDTO pixRequest;
    private PaymentRequestDTO cardRequest;

    @BeforeEach
    void setUp() {
        pendingPayment = Payment.builder()
                .id(1L)
                .debtCode(1001)
                .cpfCnpj("12345678900")
                .paymentMethod(PaymentMethod.pix)
                .amount(new BigDecimal("150.00"))
                .status(PaymentStatus.PENDENTE_PROCESSAMENTO)
                .build();

        pixRequest = new PaymentRequestDTO();
        pixRequest.setDebtCode(1001);
        pixRequest.setCpfCnpj("123.456.789-00");
        pixRequest.setPaymentMethod(PaymentMethod.pix);
        pixRequest.setAmount(new BigDecimal("150.00"));

        cardRequest = new PaymentRequestDTO();
        cardRequest.setDebtCode(1002);
        cardRequest.setCpfCnpj("123.456.789-00");
        cardRequest.setPaymentMethod(PaymentMethod.cartao_credito);
        cardRequest.setCardNumber("4111111111111111");
        cardRequest.setAmount(new BigDecimal("300.00"));
    }

    /**
     * create
     */

    @Test
    @DisplayName("Deve criar pagamento com sucesso")
    void shouldCreatePaymentSuccessfully() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

        PaymentResponseDTO response = paymentService.create(pixRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDENTE_PROCESSAMENTO);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Deve normalizar o CPF/CNPJ removendo pontuação antes de salvar")
    void shouldNormalizeCpfCnpjBeforeSaving() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

        paymentService.create(pixRequest);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCpfCnpj()).isEqualTo("12345678900");
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pagamento com CPF/CNPJ de tamanho inválido")
    void shouldThrowWhenCpfCnpjHasInvalidLength() {
        pixRequest.setCpfCnpj("123.456.789");

        assertThatThrownBy(() -> paymentService.create(pixRequest))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("CPF/CNPJ inválido");
    }

    @Test
    @DisplayName("Deve criar pagamento com cartão de crédito com sucesso e retornar número mascarado")
    void shouldCreateCardPaymentSuccessfully() {
        Payment cardPayment = Payment.builder()
                .id(2L)
                .debtCode(1002)
                .cpfCnpj("12345678900")
                .paymentMethod(PaymentMethod.cartao_credito)
                .cardNumber("4111111111111111")
                .amount(new BigDecimal("300.00"))
                .status(PaymentStatus.PENDENTE_PROCESSAMENTO)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(cardPayment);

        PaymentResponseDTO response = paymentService.create(cardRequest);

        assertThat(response).isNotNull();
        assertThat(response.getCardNumber()).isEqualTo("**** **** **** 1111");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDENTE_PROCESSAMENTO);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pagamento com cartão sem número do cartão")
    void shouldThrowWhenCardPaymentWithoutCardNumber() {
        cardRequest.setCardNumber(null);

        assertThatThrownBy(() -> paymentService.create(cardRequest))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("Número do cartão é obrigatório");
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pagamento pix com número do cartão")
    void shouldThrowWhenPixPaymentWithCardNumber() {
        pixRequest.setCardNumber("4111111111111111");

        assertThatThrownBy(() -> paymentService.create(pixRequest))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("Número do cartão não deve ser informado");
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pagamento com número de cartão inválido")
    void shouldThrowWhenCardNumberHasInvalidFormat() {
        cardRequest.setCardNumber("abc123");

        assertThatThrownBy(() -> paymentService.create(cardRequest))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("Número do cartão inválido");
    }

    /**
     * Update
     */

    @Test
    @DisplayName("Deve atualizar status de pendente para processado com sucesso")
    void shouldUpdateStatusSuccessfully() {
        Payment saved = Payment.builder()
                .id(1L)
                .debtCode(1001)
                .cpfCnpj("12345678900")
                .paymentMethod(PaymentMethod.pix)
                .amount(new BigDecimal("150.00"))
                .status(PaymentStatus.PROCESSADO_SUCESSO)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponseDTO response = paymentService.updateStatus(1L, PaymentStatus.PROCESSADO_SUCESSO);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PROCESSADO_SUCESSO);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar status de pagamento inexistente")
    void shouldThrowWhenPaymentNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updateStatus(99L, PaymentStatus.PROCESSADO_SUCESSO))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar alterar status de pagamento processado com sucesso")
    void shouldThrowWhenChangingSuccessStatus() {
        pendingPayment.setStatus(PaymentStatus.PROCESSADO_SUCESSO);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.updateStatus(1L, PaymentStatus.PENDENTE_PROCESSAMENTO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Não é possível alterar o status");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar ir de processado com falha para status inválido")
    void shouldThrowWhenInvalidTransitionFromFailure() {
        pendingPayment.setStatus(PaymentStatus.PROCESSADO_FALHA);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.updateStatus(1L, PaymentStatus.PROCESSADO_SUCESSO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Não é possível alterar o status");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar alterar status de pagamento inativo")
    void shouldThrowWhenChangingInactiveStatus() {
        pendingPayment.setStatus(PaymentStatus.INATIVO);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.updateStatus(1L, PaymentStatus.PENDENTE_PROCESSAMENTO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Não é possível alterar o status");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar inativar pagamento pendente via update de status")
    void shouldThrowWhenTryingToInactivateThroughUpdateStatus() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.updateStatus(1L, PaymentStatus.INATIVO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Não é possível alterar o status");
    }

    /**
     * delete
     */

    @Test
    @DisplayName("Deve inativar pagamento pendente com sucesso")
    void shouldDeletePaymentSuccessfully() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        paymentService.delete(1L);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.INATIVO);
    }

    @Test
    @DisplayName("Deve lançar exceção ao inativar pagamento inexistente")
    void shouldThrowWhenDeletingNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.delete(99L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar inativar pagamento não pendente")
    void shouldThrowWhenDeletingNonPendingPayment() {
        pendingPayment.setStatus(PaymentStatus.PROCESSADO_SUCESSO);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.delete(1L))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Apenas pagamentos com status Pendente de Processamento");
    }

    /**
     * findWithFilters
     */

    @Test
    @DisplayName("Deve retornar lista de pagamentos com filtros aplicados")
    void shouldReturnFilteredPayments() {
        when(paymentRepository.findAll(any(Specification.class))).thenReturn(List.of(pendingPayment));

        List<PaymentResponseDTO> result = paymentService.findWithFilters(1001, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDebtCode()).isEqualTo(1001);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há pagamentos")
    void shouldReturnEmptyListWhenNoPayments() {
        when(paymentRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<PaymentResponseDTO> result = paymentService.findWithFilters(null, null, null);

        assertThat(result).isEmpty();
    }
}
