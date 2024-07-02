package com.sparta.springmsaproduct.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProductDetailDTO {

    private String productName;
    private Integer price;
    private Integer quantity;
    private Integer likeCount;
}
