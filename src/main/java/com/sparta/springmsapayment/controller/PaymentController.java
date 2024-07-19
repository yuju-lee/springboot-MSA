package com.sparta.springmsapayment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sparta.springmsapayment.dto.PaymentRequestDTO;
import com.sparta.springmsapayment.repository.SaleTimeRepository;
import com.sparta.springmsapayment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;
    private final SaleTimeRepository saleTimeRepository;

    @Autowired
    public PaymentController(PaymentService paymentService, SaleTimeRepository saleTimeRepository) {
        this.paymentService = paymentService;
        this.saleTimeRepository = saleTimeRepository;
    }

    @PostMapping("/request")
    public ResponseEntity<String> addPaymentRequest(@RequestHeader("X-Authenticated-User") String email, @RequestBody PaymentRequestDTO requestDTO) throws JsonProcessingException {
        String result = paymentService.processPayment(email, requestDTO.getProductId(), requestDTO.getAmount());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<String> setProductSaleTime(@PathVariable int productId, @RequestParam String saleTime, @RequestHeader("X-Authenticated-User") String email) {

        try {
            paymentService.setProductSaleTime(productId, saleTime, email);
            return new ResponseEntity<>("Sale start time for product " + productId + " set to " + saleTime, HttpStatus.OK);
        } catch (NoSuchElementException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (SecurityException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception ex) {
            return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
