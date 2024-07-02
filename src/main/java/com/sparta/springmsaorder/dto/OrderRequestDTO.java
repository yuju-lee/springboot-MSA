package com.sparta.springmsaorder.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDTO {
    private List<ProductOrderDTO> productInfo;

    public List<ProductOrderDTO> getProductInfo() {
        return productInfo;
    }

    public void setProductInfo(List<ProductOrderDTO> productInfo) {
        this.productInfo = productInfo;
    }
}