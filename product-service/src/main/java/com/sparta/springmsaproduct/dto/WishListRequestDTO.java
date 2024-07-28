package com.sparta.springmsaproduct.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WishListRequestDTO {

    private Integer productId;
    private String productName;
    private int price;
    private int quantity;

    public WishListRequestDTO() {
    }

    public WishListRequestDTO(Integer productId, String productName, Integer price, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public WishListRequestDTO(String productName, Integer price, Integer quantity) {
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }
}