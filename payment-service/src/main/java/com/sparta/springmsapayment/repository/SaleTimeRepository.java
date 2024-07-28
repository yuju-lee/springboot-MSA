package com.sparta.springmsapayment.repository;

import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class SaleTimeRepository {

    private final Map<Integer, LocalTime> productSaleTimes = new HashMap<>();

    public void setProductSaleTime(int productId, LocalTime saleTime) {
        productSaleTimes.put(productId, saleTime);
    }

    public LocalTime getProductSaleTime(int productId) {
        return productSaleTimes.get(productId);
    }
}
