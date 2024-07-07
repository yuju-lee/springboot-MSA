package com.sparta.springmsapayment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sparta.springmsapayment.dto.PaymentRequestDTO;
import com.sparta.springmsapayment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/request")
    public ResponseEntity<String> addPaymentRequest(@RequestHeader("X-Authenticated-User") String email, @RequestBody PaymentRequestDTO requestDTO) throws JsonProcessingException {
        String result = paymentService.processPayment(email, requestDTO.getProductId(), requestDTO.getAmount());
        return ResponseEntity.ok(result);
    }
}
