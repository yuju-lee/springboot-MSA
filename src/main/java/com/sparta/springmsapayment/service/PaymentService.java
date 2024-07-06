package com.sparta.springmsapayment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Random;

@Service
public class PaymentService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Value("${redis.stock.key.prefix}")
    private String redisStockKeyPrefix;

    @Value("${payment.status.key}")
    private String paymentQueueKey;

    @Autowired
    public PaymentService(RedisTemplate<String, String> redisTemplate, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String processPaymentQueue() {
        String paymentDataJson = redisTemplate.opsForList().leftPop(paymentQueueKey);

        if (paymentDataJson != null) {
            try {
                Map<String, Object> paymentData = objectMapper.readValue(paymentDataJson, Map.class);
                String email = (String) paymentData.get("email");
                Integer productId = (Integer) paymentData.get("productId");
                Integer amount = (Integer) paymentData.get("amount");

                if (productId == null || amount == null) {
                    System.out.println("Invalid payment data: " + paymentData);
                    return "Invalid payment data.";
                }

                boolean paymentSuccess = random.nextDouble() >= 0.2;

                String redisKey = redisStockKeyPrefix + productId;
                if (paymentSuccess) {
                    redisTemplate.opsForValue().decrement(redisKey, amount);

                    paymentData.put("status", "COMPLETED");

                    kafkaTemplate.send("order-topic", objectMapper.writeValueAsString(paymentData));

                    System.out.println("Payment succeeded for " + email + " for product " + productId);
                    redisTemplate.opsForList().remove(paymentQueueKey, 1, paymentDataJson);

                    return "Payment succeeded for " + email + " for product " + productId;
                } else {
                    redisTemplate.opsForValue().increment(redisKey, amount);

                    paymentData.put("status", "FAILED");

                    System.out.println("Payment failed for " + email + " for product " + productId);

                    redisTemplate.opsForList().remove(paymentQueueKey, 1, paymentDataJson);

                    return "Payment failed for " + email + " for product " + productId;
                }

            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "Error processing payment data.";
            }
        }
        return "No payment data found in the queue.";
    }
}