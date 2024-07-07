package com.sparta.springmsapayment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Value("${redis.stock.key.prefix}")
    private String redisStockKeyPrefix;

    @Value("${kafka.topics.stock-update}")
    private String stockUpdateTopic;

    @Autowired
    public PaymentService(RedisTemplate<String, String> redisTemplate, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public String processPayment(String email, int productId, int amount) throws JsonProcessingException {
        String redisKey = redisStockKeyPrefix + productId;

        // 결제 진입 시 20% 확률로 실패 처리
        if (random.nextDouble() < 0.2) {
            return "[결제 진입 실패] Payment entry failed for " + email + " for product " + productId;
        }

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey + ":lock", "locked", 10, TimeUnit.SECONDS);
        if (acquired != null && acquired) {
            try {
                String stockValueStr = redisTemplate.opsForValue().get(redisKey);

                if (stockValueStr != null) {
                    Integer stock = Integer.valueOf(stockValueStr);

                    if (stock >= amount) {

                        // 결제 진행 중 20% 확률로 실패 처리
                        if (random.nextDouble() < 0.2) {
                            return "[결제 중 실패] Payment processing failed for " + email + " for product " + productId;
                        }

                        redisTemplate.opsForValue().decrement(redisKey, amount);

                        Map<String, Object> orderData = new HashMap<>();
                        orderData.put("email", email);
                        orderData.put("productId", productId);
                        orderData.put("amount", amount);
                        orderData.put("status", "COMPLETED");
                        kafkaTemplate.send("order-topic", objectMapper.writeValueAsString(orderData));

                        // Kafka를 통해 재고 동기화 이벤트 발행
                        Map<String, Object> stockUpdateData = new HashMap<>();
                        stockUpdateData.put("productId", productId);
                        stockUpdateData.put("stock", redisTemplate.opsForValue().get(redisKey));
                        kafkaTemplate.send(stockUpdateTopic, objectMapper.writeValueAsString(stockUpdateData));

                        System.out.println("Payment succeeded for " + email + " for product " + productId);
                        return "Payment succeeded for " + email + " for product " + productId;

                    } else {
                        return "재고가 부족합니다. 현재 재고: " + stock;
                    }
                } else {
                    return "재고 정보가 존재하지 않습니다. Redis key: " + redisKey;
                }
            } finally {
                redisTemplate.delete(redisKey + ":lock");
            }
        } else {
            return "Lock을 획득할 수 없습니다. 잠시 후 다시 시도해 주세요.";
        }
    }
}