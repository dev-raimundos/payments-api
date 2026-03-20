package com.fadesp.paymentsapi.controller;

import com.fadesp.paymentsapi.dto.PaymentRequestDTO;
import com.fadesp.paymentsapi.dto.PaymentResponseDTO;
import com.fadesp.paymentsapi.enums.PaymentStatus;
import com.fadesp.paymentsapi.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PaymentResponseDTO> create(@RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.status(201).body(paymentService.create(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        PaymentStatus newStatus = PaymentStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(paymentService.updateStatus(id, newStatus));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> findAll(
            @RequestParam(required = false) Integer debtCode,
            @RequestParam(required = false) String cpfCnpj,
            @RequestParam(required = false) PaymentStatus status) {

        if (debtCode != null) return ResponseEntity.ok(paymentService.findByDebtCode(debtCode));
        if (cpfCnpj != null) return ResponseEntity.ok(paymentService.findByCpfCnpj(cpfCnpj));
        if (status != null) return ResponseEntity.ok(paymentService.findByStatus(status));

        return ResponseEntity.ok(paymentService.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}