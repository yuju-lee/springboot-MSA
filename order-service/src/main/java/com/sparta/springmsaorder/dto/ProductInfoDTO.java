package com.sparta.springmsaorder.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductInfoDTO {
    private Integer id;
    private String name;
    private int price;
    private int quantity;

    public ProductInfoDTO() {
    }

    public ProductInfoDTO(Integer id, String name, int price, int quantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
}