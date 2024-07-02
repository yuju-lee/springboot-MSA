package com.sparta.springmsamember.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparta.springmsamember.dto.OrderDTO;
import com.sparta.springmsamember.dto.WishListRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class MyPageService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final List<WishListRequestDTO> wishListBuffer = new ArrayList<>();
    private final List<OrderDTO> orderBuffer = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(MyPageService.class);
    private CountDownLatch wishlistLatch;
    private CountDownLatch orderLatch;

    public MyPageService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // 위시리스트 요청 보내기
    public void sendWishListRequest(String email) {
        kafkaTemplate.send("wishlist-request-topic", email);
        wishlistLatch = new CountDownLatch(1);
    }

    // 주문 내역 요청 보내기
    public void sendOrderRequest(String email) {
        kafkaTemplate.send("order-request-topic", email);
        orderLatch = new CountDownLatch(1);
    }

    // 위시리스트 수신
    @KafkaListener(topics = "wishlist-topic", groupId = "member")
    public void receiveWishList(String wishListJson) throws JsonProcessingException {
        log.info("Received wishlist JSON: {}", wishListJson);
        try {
            WishListRequestDTO[] wishListRequestDTOs = objectMapper.readValue(wishListJson, WishListRequestDTO[].class);
            synchronized (wishListBuffer) {
                wishListBuffer.clear();
                for (WishListRequestDTO wishListRequestDTO : wishListRequestDTOs) {
                    wishListBuffer.add(wishListRequestDTO);
                    log.info("Added wish list item to buffer: {}", wishListRequestDTO);
                }
            }
            if (wishlistLatch != null) {
                wishlistLatch.countDown();
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing wishlist JSON: {}", e.getMessage());
            throw e;
        }
    }

    // 주문 내역 수신
    @KafkaListener(topics = "order-response-topic", groupId = "member")
    public void receiveOrderList(String orderListJson) throws JsonProcessingException {
        log.info("Received order list JSON: {}", orderListJson);
        try {
            OrderDTO[] orderDTOs = objectMapper.readValue(orderListJson, OrderDTO[].class);
            synchronized (orderBuffer) {
                orderBuffer.clear();
                for (OrderDTO orderDTO : orderDTOs) {
                    orderBuffer.add(orderDTO);
                    log.info("Added order item to buffer: {}", orderDTO);
                }
            }
            if (orderLatch != null) {
                orderLatch.countDown();
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing order list JSON: {}", e.getMessage());
            throw e;
        }
    }

    // 위시리스트 반환
    public List<WishListRequestDTO> getWishList() {
        try {
            if (wishlistLatch != null) {
                wishlistLatch.await(10, TimeUnit.SECONDS); // 최대 10초 대기
            }
            return new ArrayList<>(wishListBuffer);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for wishlist response");
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    // 주문 내역 반환
    public List<OrderDTO> getOrders() {
        try {
            if (orderLatch != null) {
                orderLatch.await(10, TimeUnit.SECONDS); // 최대 10초 대기
            }
            return new ArrayList<>(orderBuffer);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for order response");
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }
}