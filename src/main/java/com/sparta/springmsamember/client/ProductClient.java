package com.sparta.springmsamember.client;

import com.sparta.springmsamember.dto.WishListDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductClient {
    @GetMapping("/products/wishlist")
    List<WishListDTO> getWishList(@RequestHeader("X-Authenticated-User") String email);
}