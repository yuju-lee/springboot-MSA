package com.sparta.springmsaorder.client;

import com.sparta.springmsaorder.dto.ProductOrderDTO;
import com.sparta.springmsaorder.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductClient {

    @GetMapping("/products/detail/{productId}")
    ProductResponseDTO getProductById(@PathVariable("productId") Integer productId);

    @PostMapping("/products/restore-stock")
    void restoreStock(@RequestBody ProductOrderDTO productOrderDTO);
}