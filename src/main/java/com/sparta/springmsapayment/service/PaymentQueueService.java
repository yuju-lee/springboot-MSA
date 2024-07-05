package com.sparta.springmsapayment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class PaymentQueueService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.queue.key}")
    private String paymentQueueKey;

    private static final String REDIS_STOCK_KEY_PREFIX = "product_stock_";

    @Autowired
    public PaymentQueueService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void addPaymentRequestToQueue(String email, int productId, int amount) {
        try {
            String redisKey = REDIS_STOCK_KEY_PREFIX + productId;
            System.out.println("productId: " + productId); // Debugging line
            String stockValueStr = redisTemplate.opsForValue().get(redisKey);

            if (stockValueStr != null) {
                Integer stock = Integer.valueOf(stockValueStr);
                if (stock >= amount) {
                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("email", email);
                    paymentData.put("productId", productId);
                    paymentData.put("amount", amount);

                    String paymentDataJson = objectMapper.writeValueAsString(paymentData);
                    redisTemplate.opsForList().rightPush(paymentQueueKey, paymentDataJson);
                    System.out.println("Payment request added to the queue: " + paymentDataJson);
                } else {
                    // 재고가 부족한 경우 처리
                    System.out.println("재고가 부족합니다. 현재 재고: " + stock);
                }
            } else {
                // 재고 정보가 없는 경우 처리
                System.out.println("재고 정보가 존재하지 않습니다. Redis key: " + redisKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}