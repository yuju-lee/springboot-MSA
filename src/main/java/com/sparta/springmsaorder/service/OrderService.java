package com.sparta.springmsaorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.springmsaorder.dto.*;
import com.sparta.springmsaorder.entity.OrderDetailEntity;
import com.sparta.springmsaorder.entity.OrderEntity;
import com.sparta.springmsaorder.repository.OrderDetailRepository;
import com.sparta.springmsaorder.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final SchedulerService schedulerService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private CountDownLatch latch;
    private List<ProductResponseDTO> productResponses;

    public OrderService(OrderRepository orderRepository, OrderDetailRepository orderDetailRepository, SchedulerService schedulerService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.schedulerService = schedulerService;
    }

    public List<OrderEntity> getOrders(String email) {
        return orderRepository.findByEmail(email);
    }

    public List<OrderDetailDTO> getOrderDetails(String email, Integer orderNo) {
        OrderEntity order = orderRepository.findById(orderNo).orElseThrow(
                () -> new IllegalArgumentException("Order not found")
        );

        if (!order.getEmail().equals(email)) {
            throw new IllegalArgumentException("You do not have access to this order");
        }

        List<OrderDetailEntity> orderDetails = orderDetailRepository.findByOrderKey(orderNo);
        return orderDetails.stream()
                .map(detail -> {
                    OrderDetailDTO dto = new OrderDetailDTO();
                    dto.setProductID(detail.getProductID());
                    dto.setOrderPrice(detail.getOrderPrice());
                    dto.setProductCount(detail.getProductCount());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDTO createOrder(String email, OrderRequestDTO orderRequestDTO) {
        try {
            productResponses = new ArrayList<>();
            latch = new CountDownLatch(orderRequestDTO.getProductInfo().size());

            for (ProductOrderDTO productOrder : orderRequestDTO.getProductInfo()) {
                String requestJson = objectMapper.writeValueAsString(productOrder);
                kafkaTemplate.send("product-info-request-topic", requestJson);
            }

            latch.await(10, TimeUnit.SECONDS);

            for (ProductOrderDTO productOrder : orderRequestDTO.getProductInfo()) {
                ProductResponseDTO productResponse = productResponses.stream()
                        .filter(p -> p.getProductNo().equals(productOrder.getProductNo()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productOrder.getProductNo()));

                if (productResponse.getQuantity() < productOrder.getQty()) {
                    return new OrderResponseDTO("Order Rejected: Insufficient stock for product " + productOrder.getProductNo());
                }
            }

            // Save order entity
            OrderEntity order = new OrderEntity();
            order.setEmail(email);
            order.setOrderAt(LocalDateTime.now());
            order.setOrderStatus("ORDER_CREATE");
            OrderEntity savedOrder = orderRepository.save(order);

            // Save order detail entities and reduce stock
            for (ProductOrderDTO productOrder : orderRequestDTO.getProductInfo()) {
                ProductResponseDTO productResponse = productResponses.stream()
                        .filter(p -> p.getProductNo().equals(productOrder.getProductNo()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productOrder.getProductNo()));

                OrderDetailEntity orderDetail = new OrderDetailEntity();
                orderDetail.setOrderKey(savedOrder.getOrderKey());
                orderDetail.setProductID(productResponse.getProductNo());
                orderDetail.setOrderPrice(productResponse.getPrice() * productOrder.getQty());
                orderDetail.setProductCount(productOrder.getQty());
                orderDetailRepository.save(orderDetail);

                String deductStockJson = objectMapper.writeValueAsString(productOrder);
                kafkaTemplate.send("deduct-stock-request-topic", deductStockJson);
            }

            // Schedule next status update
            schedulerService.scheduleNextStatusUpdate(savedOrder, "ON_DELIVERY", 24);

            return new OrderResponseDTO("Order Created Successfully");
        } catch (Exception e) {
            return new OrderResponseDTO("Order Failed: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "product-info-response-topic", groupId = "order")
    public void receiveProductInfo(String productInfoJson) throws JsonProcessingException {
        ProductResponseDTO productResponse = objectMapper.readValue(productInfoJson, ProductResponseDTO.class);
        productResponses.add(productResponse);
        latch.countDown();
    }

    @Transactional
    public void deleteOrder(String email, Integer orderNo) {
        OrderEntity order = orderRepository.findById(orderNo).orElseThrow(
                () -> new IllegalArgumentException("Order not found")
        );

        if (!order.getEmail().equals(email)) {
            throw new IllegalArgumentException("You do not have access to this order");
        }

        if (!"ORDER_CREATE".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Order cancellation is only possible before shipping.");
        }

        List<OrderDetailEntity> orderDetails = orderDetailRepository.findByOrderKey(orderNo);
        for (OrderDetailEntity orderDetail : orderDetails) {
            ProductOrderDTO productOrderDTO = new ProductOrderDTO();
            productOrderDTO.setProductNo(orderDetail.getProductID());
            productOrderDTO.setQty(orderDetail.getProductCount());
            try {
                String restoreStockJson = objectMapper.writeValueAsString(productOrderDTO);
                kafkaTemplate.send("restore-stock-request-topic", restoreStockJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing product order for stock restoration", e);
            }
        }
        order.setOrderStatus("CANCEL_COMPLETED");
        orderRepository.save(order);
    }

    @Transactional
    public void returnOrder(String email, Integer orderNo) {
        OrderEntity order = orderRepository.findById(orderNo).orElseThrow(
                () -> new IllegalArgumentException("Order not found")
        ); if (!order.getEmail().equals(email)) {
            throw new IllegalArgumentException("You do not have access to this order");
        }

        if (!order.getOrderStatus().equals("ORDER_COMPLETE")) {
            throw new IllegalArgumentException("Return is only allowed for completed orders.");
        }

        List<OrderDetailEntity> orderDetails = orderDetailRepository.findByOrderKey(orderNo);
        for (OrderDetailEntity orderDetail : orderDetails) {
            ProductOrderDTO productOrderDTO = new ProductOrderDTO();
            productOrderDTO.setProductNo(orderDetail.getProductID());
            productOrderDTO.setQty(orderDetail.getProductCount());
            try {
                String restoreStockJson = objectMapper.writeValueAsString(productOrderDTO);
                kafkaTemplate.send("restore-stock-request-topic", restoreStockJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing product order for stock restoration", e);
            }
        }

        order.setOrderStatus("RETURN_COMPLETE");
        orderRepository.save(order);
    }

    @KafkaListener(topics = "order-request-topic", groupId = "order")
    public void handleOrderRequest(String email) {
        List<OrderEntity> orders = orderRepository.findByEmail(email);
        List<OrderDTO> orderDTOs = orders.stream().map(order -> new OrderDTO(order.getOrderKey(), order.getOrderAt(), order.getOrderStatus())).collect(Collectors.toList());
        try {
            String orderListJson = objectMapper.writeValueAsString(orderDTOs);
            kafkaTemplate.send("order-response-topic", orderListJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing order list", e);
        }
    }
}