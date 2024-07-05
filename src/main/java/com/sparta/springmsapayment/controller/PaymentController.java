package com.sparta.springmsapayment.controller;

import com.sparta.springmsapayment.dto.PaymentRequestDTO;
import com.sparta.springmsapayment.service.PaymentQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentQueueService paymentQueueService;

    @Autowired
    public PaymentController(PaymentQueueService paymentQueueService) {
        this.paymentQueueService = paymentQueueService;
    }

    @PostMapping("/request")
    public ResponseEntity<String> addPaymentRequest(@RequestHeader("X-Authenticated-User") String email, @RequestBody PaymentRequestDTO requestDTO) {
        System.out.println("Request received: " + requestDTO); // 전체 DTO 출력
        System.out.println("ProductId: " + requestDTO.getProductId());
        System.out.println("Amount: " + requestDTO.getAmount());

        paymentQueueService.addPaymentRequestToQueue(email, requestDTO.getProductId(), requestDTO.getAmount());
        return ResponseEntity.ok("Payment request added to the queue");
    }
}