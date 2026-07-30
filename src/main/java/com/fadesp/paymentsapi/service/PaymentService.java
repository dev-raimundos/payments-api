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
import com.fadesp.paymentsapi.specification.PaymentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PaymentStatus.PENDENTE_PROCESSAMENTO,
                EnumSet.of(PaymentStatus.PROCESSADO_SUCESSO, PaymentStatus.PROCESSADO_FALHA));
        ALLOWED_TRANSITIONS.put(PaymentStatus.PROCESSADO_FALHA,
                EnumSet.of(PaymentStatus.PENDENTE_PROCESSAMENTO));
        ALLOWED_TRANSITIONS.put(PaymentStatus.PROCESSADO_SUCESSO, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED_TRANSITIONS.put(PaymentStatus.INATIVO, EnumSet.noneOf(PaymentStatus.class));
    }

    private final PaymentRepository paymentRepository;

    public PaymentResponseDTO create(PaymentRequestDTO dto) {
        validateCardNumber(dto);
        String normalizedCpfCnpj = normalizeAndValidateCpfCnpj(dto.getCpfCnpj());

        Payment payment = Payment.builder()
                .debtCode(dto.getDebtCode())
                .cpfCnpj(normalizedCpfCnpj)
                .paymentMethod(dto.getPaymentMethod())
                .cardNumber(dto.getCardNumber() == null ? null : dto.getCardNumber().replaceAll("\\s+", ""))
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
        String normalizedCpfCnpj = cpfCnpj == null || cpfCnpj.isBlank() ? null : cpfCnpj.replaceAll("\\D", "");

        Specification<Payment> spec = Specification
                .allOf(
                        PaymentSpecification.hasDebtCode(debtCode),
                        PaymentSpecification.hasCpfCnpj(normalizedCpfCnpj),
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
            throw new PaymentValidationException(
                    "Número do cartão é obrigatório para pagamentos com cartão."
            );
        }

        if (!isCardMethod && dto.getCardNumber() != null) {
            throw new PaymentValidationException(
                    "Número do cartão não deve ser informado para este método de pagamento."
            );
        }

        if (isCardMethod && !dto.getCardNumber().replaceAll("\\s+", "").matches("\\d{13,19}")) {
            throw new PaymentValidationException(
                    "Número do cartão inválido: deve conter apenas dígitos (13 a 19)."
            );
        }
    }

    private String normalizeAndValidateCpfCnpj(String cpfCnpj) {
        String digits = cpfCnpj.replaceAll("\\D", "");

        if (digits.length() != 11 && digits.length() != 14) {
            throw new PaymentValidationException(
                    "CPF/CNPJ inválido: deve conter 11 (CPF) ou 14 (CNPJ) dígitos."
            );
        }

        return digits;
    }

    private void validateStatusTransition(PaymentStatus current, PaymentStatus next) {
        Set<PaymentStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.get(current);

        if (allowedNextStatuses == null || !allowedNextStatuses.contains(next)) {
            throw new InvalidStatusTransitionException(
                    "Não é possível alterar o status de " + current + " para " + next + "."
            );
        }
    }

    private PaymentResponseDTO toResponseDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .debtCode(payment.getDebtCode())
                .cpfCnpj(payment.getCpfCnpj())
                .paymentMethod(payment.getPaymentMethod())
                .cardNumber(maskCardNumber(payment.getCardNumber()))
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}