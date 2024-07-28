package com.sparta.springmsapayment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductResponseDTO {
    private String email;
    private int productNo;
    private String productName;
    private double price;
    private int quantity;
}