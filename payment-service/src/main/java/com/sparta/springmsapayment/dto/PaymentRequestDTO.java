package com.sparta.springmsapayment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequestDTO {
    private int productId;
    private int amount;
}