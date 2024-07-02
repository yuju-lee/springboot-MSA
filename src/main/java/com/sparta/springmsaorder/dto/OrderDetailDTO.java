package com.sparta.springmsaorder.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDetailDTO {

    private Integer productID;
    private Integer orderPrice;
    private Integer productCount;

    public OrderDetailDTO(Integer productID, Integer orderPrice, Integer productCount) {
        this.productID = productID;
        this.orderPrice = orderPrice;
        this.productCount = productCount;
    }

    public OrderDetailDTO() {

    }
}