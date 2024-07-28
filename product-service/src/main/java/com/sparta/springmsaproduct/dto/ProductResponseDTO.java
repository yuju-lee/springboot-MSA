package com.sparta.springmsaproduct.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductResponseDTO {
    private Integer productNo;
    private String productName;
    private Integer price;
    private Integer quantity;

    public ProductResponseDTO(Integer productId, String productName, Integer price, Integer quantity) {
        this.productNo = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public ProductResponseDTO() {

    }
}
