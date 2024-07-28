package com.sparta.springmsaorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.springmsaorder.client.ProductClient;
import com.sparta.springmsaorder.dto.*;
import com.sparta.springmsaorder.entity.OrderDetailEntity;
import com.sparta.springmsaorder.entity.OrderEntity;
import com.sparta.springmsaorder.repository.OrderDetailRepository;
import com.sparta.springmsaorder.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private ProductClient productClient;

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

    public OrderResponseDTO createOrder(String email, OrderRequestDTO orderRequestDTO) {
        System.out.println("Start Create order");
        try {
            List<Integer> productIds = orderRequestDTO.getProductInfo().stream()
                    .map(ProductOrderDTO::getProductNo)
                    .collect(Collectors.toList());

            List<ProductResponseDTO> productResponses = productIds.stream()
                    .map(productClient::getProductById)
                    .collect(Collectors.toList());

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

            // Save order detail entities
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
            }

            // Schedule next status update
            schedulerService.scheduleNextStatusUpdate(savedOrder, "ON_DELIVERY", 24);

            return new OrderResponseDTO("Order Created Successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return new OrderResponseDTO("Order Failed: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteOrder(String email, Integer orderNo) {
        cancelOrReturnOrder(email, orderNo, "ORDER_CREATE", "CANCEL_COMPLETED");
    }

    @Transactional
    public void returnOrder(String email, Integer orderNo) {
        cancelOrReturnOrder(email, orderNo, "ORDER_COMPLETE", "RETURN_COMPLETE");
    }

    public void cancelOrReturnOrder(String email, Integer orderNo, String validStatus, String finalStatus) {
        OrderEntity order = orderRepository.findById(orderNo).orElseThrow(
                () -> new IllegalArgumentException("Order not found")
        );

        if (!order.getEmail().equals(email)) {
            throw new IllegalArgumentException("You do not have access to this order");
        }

        if (!order.getOrderStatus().equals(validStatus)) {
            throw new IllegalArgumentException("Order cannot be processed. Current status: " + order.getOrderStatus());
        }

        List<OrderDetailEntity> orderDetails = orderDetailRepository.findByOrderKey(orderNo);
        for (OrderDetailEntity orderDetail : orderDetails) {
            ProductOrderDTO productOrderDTO = new ProductOrderDTO();
            productOrderDTO.setProductNo(orderDetail.getProductID());
            productOrderDTO.setQty(orderDetail.getProductCount());
            productClient.restoreStock(productOrderDTO);
        }
        order.setOrderStatus(finalStatus);
        orderRepository.save(order);
    }

    @KafkaListener(topics = "order-topic", groupId = "order")
    public void handleOrderEvent(String orderDataJson) {
        try {
            Map orderData = objectMapper.readValue(orderDataJson, Map.class);
            String email = (String) orderData.get("email");
            int productId = Integer.parseInt((String) orderData.get("productId"));
            int amount = Integer.parseInt((String) orderData.get("amount"));
            String status = (String) orderData.get("status");

            if ("COMPLETED".equals(status)) {
                System.out.println("Order request received: " + email + " " + amount + " " + productId);
                OrderRequestDTO orderRequestDTO = new OrderRequestDTO();
                orderRequestDTO.setEmail(email);

                ProductOrderDTO productOrderDTO = new ProductOrderDTO();
                productOrderDTO.setProductNo(productId);
                productOrderDTO.setQty(amount);

                List<ProductOrderDTO> productOrderDTOList = new ArrayList<>();
                productOrderDTOList.add(productOrderDTO);

                orderRequestDTO.setProductInfo(productOrderDTOList);

                OrderResponseDTO response = createOrder(email, orderRequestDTO);
                System.out.println("Order creation result: " + response.getMessage());
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Error parsing productId or amount: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error in handleOrderEvent: " + e.getMessage());
            e.printStackTrace();
        }
    }
}