package com.sparta.springmsamember.client;

import com.sparta.springmsamember.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "order-service", url = "${order-service.url}")
public interface OrderClient {
    @GetMapping("/order")
    List<OrderDTO> getOrders(@RequestHeader("X-Authenticated-User") String email);
}