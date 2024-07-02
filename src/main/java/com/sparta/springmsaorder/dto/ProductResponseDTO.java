package com.sparta.springmsaorder.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductResponseDTO {
    private Integer productNo;
    private String productName;
    private Integer price;
    private Integer quantity;

}