package com.sparta.springmsapayment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.queue.key}")
    private String paymentQueueKey;

    private static final String REDIS_STOCK_KEY_PREFIX = "product_stock_";

    @Autowired
    public PaymentService(RedisTemplate<String, String> redisTemplate,
                          KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 1000) // 1초마다 대기열 확인
    public void processPaymentQueue() {
        String paymentDataJson = redisTemplate.opsForList().leftPop(paymentQueueKey);
        if (paymentDataJson != null) {
            try {
                Map<String, Object> paymentData = objectMapper.readValue(paymentDataJson, Map.class);
                String email = (String) paymentData.get("email");
                int productID = (Integer) paymentData.get("productID");
                int amount = (Integer) paymentData.get("amount");

                // 20% 확률로 결제 실패 시뮬레이션
                if (Math.random() < 0.2) {
                    // 결제 실패
                    restoreStock(productID, amount);
                    sendPaymentFailureEvent(paymentDataJson);
                } else {
                    // 결제 성공
                    reserveStock(productID, amount);
                    sendPaymentSuccessEvent(paymentDataJson);
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    private void reserveStock(int productID, int amount) {
        redisTemplate.opsForValue().decrement(REDIS_STOCK_KEY_PREFIX + productID, amount);
        // MySQL에서도 재고 감소 필요 시 추가 로직 작성
    }

    private void restoreStock(int productID, int amount) {
        redisTemplate.opsForValue().increment(REDIS_STOCK_KEY_PREFIX + productID, amount);
        // MySQL에서도 재고 복구 필요 시 추가 로직 작성
    }

    private void sendPaymentSuccessEvent(String paymentDataJson) {
        kafkaTemplate.send("order-request-topic", paymentDataJson);
    }

    private void sendPaymentFailureEvent(String paymentDataJson) {
        kafkaTemplate.send("payment-failure-topic", paymentDataJson);
    }
}