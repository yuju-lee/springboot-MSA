package com.sparta.springmsapayment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class PaymentQueueService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final Random random = new Random();

    @Value("${payment.queue.key}")
    private String paymentQueueKey;

    @Value("${redis.stock.key.prefix}")
    private String redisStockKeyPrefix;

    @Autowired
    public PaymentQueueService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, PaymentService paymentService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    public String addPaymentRequestToQueue(String email, int productId, int amount) {
        try {
            String redisKey = redisStockKeyPrefix + productId;
            System.out.println("productId:" + productId);
            String stockValueStr = redisTemplate.opsForValue().get(redisKey);

            if (stockValueStr != null) {
                Integer stock = Integer.valueOf(stockValueStr);
                if (stock >= amount) {
                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("email", email);
                    paymentData.put("productId", productId);
                    paymentData.put("amount", amount);
                    paymentData.put("status", "PENDING");

                    String paymentDataJson = objectMapper.writeValueAsString(paymentData);
                    redisTemplate.opsForList().rightPush(paymentQueueKey, paymentDataJson);
                    System.out.println("Payment request added to the queue: " + paymentDataJson);

                    // 요청이 들어올 때마다 대기열을 확인하고 결제 요청 처리
                    return paymentService.processPaymentQueue();
                } else {
                    // 재고가 부족한 경우 처리
                    System.out.println("재고가 부족합니다. 현재 재고: " + stock);
                    return "재고가 부족합니다. 현재 재고: " + stock;
                }
            } else {
                // 재고 정보가 없는 경우 처리
                System.out.println("재고 정보가 존재하지 않습니다. Redis key: " + redisKey);
                return "재고 정보가 존재하지 않습니다. Redis key: " + redisKey;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "결제 요청 중 오류가 발생했습니다.";
        }
    }
}