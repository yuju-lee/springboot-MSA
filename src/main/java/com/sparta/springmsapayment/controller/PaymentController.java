package com.sparta.springmsapayment.controller;

import com.sparta.springmsapayment.dto.PaymentRequestDTO;
import com.sparta.springmsapayment.service.PaymentQueueService;
import com.sparta.springmsapayment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentQueueService paymentQueueService;
    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentQueueService paymentQueueService, PaymentService paymentService) {
        this.paymentQueueService = paymentQueueService;
        this.paymentService = paymentService;
    }

    @PostMapping("/request")
    public ResponseEntity<String> addPaymentRequest(@RequestHeader("X-Authenticated-User") String email, @RequestBody PaymentRequestDTO requestDTO) {
        String result = paymentQueueService.addPaymentRequestToQueue(email, requestDTO.getProductId(), requestDTO.getAmount());
        return ResponseEntity.ok(result);
    }

}
