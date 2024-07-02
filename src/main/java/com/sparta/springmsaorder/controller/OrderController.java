package com.sparta.springmsaorder.controller;

import com.sparta.springmsaorder.dto.OrderDetailDTO;
import com.sparta.springmsaorder.dto.OrderRequestDTO;
import com.sparta.springmsaorder.dto.OrderResponseDTO;
import com.sparta.springmsaorder.entity.OrderEntity;
import com.sparta.springmsaorder.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderEntity>> getOrders(@RequestHeader("X-Authenticated-User") String email) {
        List<OrderEntity> orders = orderService.getOrders(email);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestHeader("X-Authenticated-User") String email, @RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO response = orderService.createOrder(email, orderRequestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderNo}")
    public ResponseEntity<List<OrderDetailDTO>> getOrderDetails(@RequestHeader("X-Authenticated-User") String email, @PathVariable Integer orderNo) {
        List<OrderDetailDTO> orderDetails = orderService.getOrderDetails(email, orderNo);
        return ResponseEntity.ok(orderDetails);
    }

    @DeleteMapping("/{orderNo}")
    public ResponseEntity<?> cancelOrder(@RequestHeader("X-Authenticated-User") String email, @PathVariable Integer orderNo) {
        try {
            orderService.deleteOrder(email, orderNo);
            return ResponseEntity.ok("Order cancellation completed successfully. Order number is: " + orderNo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/return/{orderNo}")
    public ResponseEntity<?> returnOrder(@RequestHeader("X-Authenticated-User") String email, @PathVariable Integer orderNo) {
        try {
            orderService.returnOrder(email, orderNo);
            return ResponseEntity.ok("Order return completed successfully. Order number is: " + orderNo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}