package com.sparta.springmsapayment.dto;

import lombok.Getter;
import lombok.Setter;

public class PaymentRequestDTO {
    private int productId;
    private int amount;

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}