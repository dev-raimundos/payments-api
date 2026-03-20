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
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentResponseDTO create(PaymentRequestDTO dto) {
        validateCardNumber(dto);

        Payment payment = Payment.builder()
                .debtCode(dto.getDebtCode())
                .cpfCnpj(dto.getCpfCnpj())
                .paymentMethod(dto.getPaymentMethod())
                .cardNumber(dto.getCardNumber())
                .amount(dto.getAmount())
                .status(PaymentStatus.PENDENTE_PROCESSAMENTO)
                .build();

        return toResponseDTO(paymentRepository.save(payment));
    }

    public PaymentResponseDTO updateStatus(Long id, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        validateStatusTransition(payment.getStatus(), newStatus);
        payment.setStatus(newStatus);

        return toResponseDTO(paymentRepository.save(payment));
    }

    public List<PaymentResponseDTO> findWithFilters(Integer debtCode, String cpfCnpj, PaymentStatus status) {
        Specification<Payment> spec = Specification
                .allOf(
                        PaymentSpecification.hasDebtCode(debtCode),
                        PaymentSpecification.hasCpfCnpj(cpfCnpj),
                        PaymentSpecification.hasStatus(status)
                );

        return paymentRepository.findAll(spec)
                .stream()
                .map(this::toResponseDTO).toList();
    }

    public void delete(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() != PaymentStatus.PENDENTE_PROCESSAMENTO) {
            throw new InvalidStatusTransitionException(
                    "Apenas pagamentos com status Pendente de Processamento podem ser inativados."
            );
        }

        payment.setStatus(PaymentStatus.INATIVO);
        paymentRepository.save(payment);
    }

    private void validateCardNumber(PaymentRequestDTO dto) {
        boolean isCardMethod = dto.getPaymentMethod() == PaymentMethod.cartao_credito
                || dto.getPaymentMethod() == PaymentMethod.cartao_debito;

        if (isCardMethod && (dto.getCardNumber() == null || dto.getCardNumber().isBlank())) {
            throw new InvalidStatusTransitionException(
                    "Número do cartão é obrigatório para pagamentos com cartão."
            );
        }

        if (!isCardMethod && dto.getCardNumber() != null) {
            throw new InvalidStatusTransitionException(
                    "Número do cartão não deve ser informado para este método de pagamento."
            );
        }
    }

    private void validateStatusTransition(PaymentStatus current, PaymentStatus next) {
        if (current == PaymentStatus.PROCESSADO_SUCESSO) {
            throw new InvalidStatusTransitionException(
                    "Pagamentos processados com sucesso não podem ter o status alterado."
            );
        }

        if (current == PaymentStatus.PROCESSADO_FALHA && next != PaymentStatus.PENDENTE_PROCESSAMENTO) {
            throw new InvalidStatusTransitionException(
                    "Pagamentos processados com falha só podem voltar para Pendente de Processamento."
            );
        }

        if (current == PaymentStatus.INATIVO) {
            throw new InvalidStatusTransitionException(
                    "Pagamentos inativos não podem ter o status alterado."
            );
        }
    }

    private PaymentResponseDTO toResponseDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .debtCode(payment.getDebtCode())
                .cpfCnpj(payment.getCpfCnpj())
                .paymentMethod(payment.getPaymentMethod())
                .cardNumber(payment.getCardNumber())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build();
    }
}