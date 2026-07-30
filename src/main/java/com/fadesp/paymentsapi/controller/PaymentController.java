package com.fadesp.paymentsapi.controller;

import com.fadesp.paymentsapi.dto.PaymentRequestDTO;
import com.fadesp.paymentsapi.dto.PaymentResponseDTO;
import com.fadesp.paymentsapi.dto.UpdateStatusRequestDTO;
import com.fadesp.paymentsapi.enums.PaymentStatus;
import com.fadesp.paymentsapi.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PaymentResponseDTO> create(@Valid @RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.status(201).body(paymentService.create(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequestDTO body) {
        return ResponseEntity.ok(paymentService.updateStatus(id, body.getStatus()));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> findAll(
            @RequestParam(required = false) Integer debtCode,
            @RequestParam(required = false) String cpfCnpj,
            @RequestParam(required = false) PaymentStatus status) {
        return ResponseEntity.ok(paymentService.findWithFilters(debtCode, cpfCnpj, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}