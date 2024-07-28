package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WishListDTO {
    private int productId;
    private String productName;
    private int price;
    private int quantity;

}