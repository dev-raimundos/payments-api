package com.fadesp.paymentsapi.repository;

import com.fadesp.paymentsapi.enums.PaymentStatus;
import com.fadesp.paymentsapi.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByDebtCode(Integer debtCode);
    List<Payment> findByCpfCnpj(String cpfCnpj);
    List<Payment> findByStatus(PaymentStatus status);
}

