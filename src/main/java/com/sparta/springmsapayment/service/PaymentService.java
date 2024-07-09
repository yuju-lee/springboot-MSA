package com.sparta.springmsapayment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.springmsapayment.repository.SaleTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private final SaleTimeRepository saleTimeRepository;

    @Value("${redis.stock.key.prefix}")
    private String redisStockKeyPrefix;

    @Value("${kafka.topics.stock-update}")
    private String stockUpdateTopic;

    @Autowired
    public PaymentService(RedisTemplate<String, String> redisTemplate, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, SaleTimeRepository saleTimeRepository) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.saleTimeRepository = saleTimeRepository;
    }

    @Transactional
    public String processPayment(String email, int productId, int amount) throws JsonProcessingException {
        String redisKey = redisStockKeyPrefix + productId;

        // 특정 제품의 판매 시작 시간 확인
        LocalTime saleStartTime = saleTimeRepository.getProductSaleTime(productId);
        LocalTime now = LocalTime.now();
        if (saleStartTime != null && now.isBefore(saleStartTime)) {
            return "결제는 " + saleStartTime.toString() + "부터 가능합니다.";
        }

        // 결제 진입 시 20% 확률로 실패 처리
        if (random.nextDouble() < 0.2) {
            return "[결제 진입 실패] Payment entry failed for " + email + " for product " + productId;
        }

        // Lua 스크립트를 사용하여 재고 확인, 감소, 주문 생성을 원자적으로 수행
        String luaScript =
                "local stock = redis.call('get', KEYS[1]) " +
                        "if stock and tonumber(stock) >= tonumber(ARGV[1]) then " +
                        "   redis.call('decrby', KEYS[1], ARGV[1]) " +
                        "   local order = {email=ARGV[2], productId=ARGV[3], amount=ARGV[1], status='PENDING'} " +
                        "   redis.call('rpush', KEYS[2], cjson.encode(order)) " +
                        "   return 1 " +
                        "else " +
                        "   return 0 " +
                        "end";

        List<String> keys = Arrays.asList(redisKey, "pending_orders");
        List<String> args = Arrays.asList(String.valueOf(amount), email, String.valueOf(productId));

        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class), keys, args.toArray());

        if (result == 1) {
            // 결제 진행 중 20% 확률로 실패 처리
            if (random.nextDouble() < 0.2) {
                // 재고 복구 및 주문 취소
                String rollbackScript =
                        "redis.call('incrby', KEYS[1], ARGV[1]) " +
                                "local orders = redis.call('lrange', KEYS[2], -1, -1) " +
                                "if #orders > 0 then " +
                                "   redis.call('rpop', KEYS[2]) " +
                                "end " +
                                "return 1";

                redisTemplate.execute(new DefaultRedisScript<>(rollbackScript, Long.class), keys, String.valueOf(amount));
                return "[결제 중 실패] Payment processing failed for " + email + " for product " + productId;
            }

            // 주문 완료 처리 및 재고 동기화
            processPendingOrders();

            System.out.println("Payment succeeded for " + email + " for product " + productId);
            return "Payment succeeded for " + email + " for product " + productId;
        } else {
            String currentStock = redisTemplate.opsForValue().get(redisKey);
            return "재고가 부족합니다. 현재 재고: " + currentStock;
        }
    }

    private void processPendingOrders() throws JsonProcessingException {
        while (true) {
            String orderJson = redisTemplate.opsForList().leftPop("pending_orders");
            if (orderJson == null) {
                break;
            }

            Map<String, Object> order = objectMapper.readValue(orderJson, Map.class);
            order.put("status", "COMPLETED");

            // Kafka 이벤트 발행
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("email", order.get("email"));
            orderData.put("productId", order.get("productId"));
            orderData.put("amount", order.get("amount"));
            orderData.put("status", "COMPLETED");

            kafkaTemplate.send("order-topic", objectMapper.writeValueAsString(orderData));

            // Kafka를 통해 재고 동기화 이벤트 발행
            String redisKey = redisStockKeyPrefix + order.get("productId");
            Map<String, Object> stockUpdateData = new HashMap<>();
            stockUpdateData.put("productId", order.get("productId"));
            stockUpdateData.put("stock", redisTemplate.opsForValue().get(redisKey));
            kafkaTemplate.send(stockUpdateTopic, objectMapper.writeValueAsString(stockUpdateData));
        }
    }
}