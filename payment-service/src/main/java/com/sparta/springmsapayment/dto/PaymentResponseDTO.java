package com.sparta.springmsapayment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponseDTO {
    private boolean success;
    private String message;

    public PaymentResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}