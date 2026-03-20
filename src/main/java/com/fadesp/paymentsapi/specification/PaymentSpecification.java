package com.fadesp.paymentsapi.specification;

import com.fadesp.paymentsapi.enums.PaymentStatus;
import com.fadesp.paymentsapi.model.Payment;
import org.springframework.data.jpa.domain.Specification;

public class PaymentSpecification {

    private PaymentSpecification() {
    }

    public static Specification<Payment> hasDebtCode(Integer debtCode) {
        return (root, query, cb) ->
                debtCode == null ? null : cb.equal(root.get("debtCode"), debtCode);
    }

    public static Specification<Payment> hasCpfCnpj(String cpfCnpj) {
        return (root, query, cb) ->
                cpfCnpj == null ? null : cb.equal(root.get("cpfCnpj"), cpfCnpj);
    }

    public static Specification<Payment> hasStatus(PaymentStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }
}