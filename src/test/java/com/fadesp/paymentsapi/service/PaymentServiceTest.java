package com.fadesp.paymentsapi.service;

import com.fadesp.paymentsapi.dto.PaymentRequestDTO;
import com.fadesp.paymentsapi.dto.PaymentResponseDTO;
import com.fadesp.paymentsapi.enums.PaymentMethod;
import com.fadesp.paymentsapi.enums.PaymentStatus;
import com.fadesp.paymentsapi.exception.InvalidStatusTransitionException;
import com.fadesp.paymentsapi.exception.PaymentNotFoundException;
import com.fadesp.paymentsapi.model.Payment;
import com.fadesp.paymentsapi.repository.PaymentRepository;
import com.fadesp.paymentsapi.specification.PaymentSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                .cpfCnpj("123.456.789-00")
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
    @DisplayName("Deve criar pagamento com cartão de crédito com sucesso")
    void shouldCreateCardPaymentSuccessfully() {
        Payment cardPayment = Payment.builder()
                .id(2L)
                .debtCode(1002)
                .cpfCnpj("123.456.789-00")
                .paymentMethod(PaymentMethod.cartao_credito)
                .cardNumber("4111111111111111")
                .amount(new BigDecimal("300.00"))
                .status(PaymentStatus.PENDENTE_PROCESSAMENTO)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(cardPayment);

        PaymentResponseDTO response = paymentService.create(cardRequest);

        assertThat(response).isNotNull();
        assertThat(response.getCardNumber()).isEqualTo("4111111111111111");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDENTE_PROCESSAMENTO);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pagamento com cartão sem número do cartão")
    void shouldThrowWhenCardPaymentWithoutCardNumber() {
        cardRequest.setCardNumber(null);

        assertThatThrownBy(() -> paymentService.create(cardRequest))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Número do cartão é obrigatório");
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pagamento pix com número do cartão")
    void shouldThrowWhenPixPaymentWithCardNumber() {
        pixRequest.setCardNumber("4111111111111111");

        assertThatThrownBy(() -> paymentService.create(pixRequest))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Número do cartão não deve ser informado");
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
                .cpfCnpj("123.456.789-00")
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
                .hasMessageContaining("processados com sucesso não podem ter o status alterado");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar ir de processado com falha para status inválido")
    void shouldThrowWhenInvalidTransitionFromFailure() {
        pendingPayment.setStatus(PaymentStatus.PROCESSADO_FALHA);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.updateStatus(1L, PaymentStatus.PROCESSADO_SUCESSO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("só podem voltar para Pendente de Processamento");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar alterar status de pagamento inativo")
    void shouldThrowWhenChangingInactiveStatus() {
        pendingPayment.setStatus(PaymentStatus.INATIVO);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.updateStatus(1L, PaymentStatus.PENDENTE_PROCESSAMENTO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("inativos não podem ter o status alterado");
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