package com.sparta.springmsapayment.service;

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

@Service
public class PaymentService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();

    @Value("${payment.queue.key}")
    private String paymentQueueKey;

    @Value("${product.stock.redis.key.prefix}")
    private String redisStockKeyPrefix;

    @Autowired
    public PaymentService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void addPaymentRequestToQueue(String email, int productId, int amount) {
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

                    String paymentDataJson = objectMapper.writeValueAsString(paymentData);
                    redisTemplate.opsForList().rightPush(paymentQueueKey, paymentDataJson);
                    System.out.println("Payment request added to the queue: " + paymentDataJson);

                    // 요청이 들어올 때마다 대기열을 확인하고 결제 요청 처리
                    processPaymentQueue();
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

    @Transactional
    public void processPaymentQueue() {
        String paymentDataJson = redisTemplate.opsForList().leftPop(paymentQueueKey);
        System.out.println("paymentDataJson: "+paymentDataJson);
        if (paymentDataJson != null) {
            try {
                Map<String, Object> paymentData = objectMapper.readValue(paymentDataJson, Map.class);
                String email = (String) paymentData.get("email");
                Integer productIdObj = (Integer) paymentData.get("productId");
                Integer amountObj = (Integer) paymentData.get("amount");

                // Null check for productId and amount
                if (productIdObj == null || amountObj == null) {
                    System.out.println("Invalid payment data: " + paymentData);
                    return;
                }

                int productId = productIdObj.intValue();
                int amount = amountObj.intValue();

                // 결제 실패 확률 20% 적용
                boolean paymentSuccess = random.nextDouble() >= 0.2;

                String redisKey = redisStockKeyPrefix + productId;
                if (paymentSuccess) {
                    // 결제 성공 시 재고 차감
                    redisTemplate.opsForValue().decrement(redisKey, amount);

                    // 결제 성공 메시지 생성
                    Map<String, Object> orderData = Map.of("email", email, "productId", productId, "amount", amount, "status", "COMPLETED");
                    String orderDataJson = objectMapper.writeValueAsString(orderData);

                    // Kafka를 통해 Order Service로 전송
                    kafkaTemplate.send("order-topic", orderDataJson);

                    System.out.println("Payment succeeded for " + email + " for product " + productId);
                } else {
                    // 결제 실패 시 재고 복구
                    redisTemplate.opsForValue().increment(redisKey, amount);

                    System.out.println("Payment failed for " + email + " for product " + productId);
                }

                // 결제 완료 후 대기열에서 해당 주문 정보 삭제
                redisTemplate.opsForList().remove(paymentQueueKey, 1, paymentDataJson);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}