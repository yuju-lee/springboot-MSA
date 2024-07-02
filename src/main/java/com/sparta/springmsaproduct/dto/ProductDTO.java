package com.sparta.springmsaproduct.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDTO {

    private String productName;
    private Integer price;

    public ProductDTO(String productName, Integer price) {
        this.productName = productName;
        this.price = price;
    }

}