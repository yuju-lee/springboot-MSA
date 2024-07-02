package com.sparta.springmsaorder.service;

import com.sparta.springmsaorder.entity.OrderEntity;
import com.sparta.springmsaorder.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchedulerService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private final OrderRepository orderRepository;
    private final TaskScheduler taskScheduler;

    public SchedulerService(OrderRepository orderRepository, TaskScheduler taskScheduler) {
        this.orderRepository = orderRepository;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void scheduleInitialOrderStatusUpdates() {
        List<OrderEntity> orders = orderRepository.findByOrderStatus("ORDER_CREATE");
        for (OrderEntity order : orders) {
            scheduleNextStatusUpdate(order, "ON_DELIVERY", 24);
        }
    }

    public void scheduleNextStatusUpdate(OrderEntity order, String nextStatus, int hours) {
        LocalDateTime updateTime = order.getOrderAt().plusHours(hours);
        long delay = java.time.Duration.between(LocalDateTime.now(), updateTime).toMillis();

        taskScheduler.schedule(() -> {
            updateOrderStatus(order.getOrderKey(), nextStatus);
        }, new java.util.Date(System.currentTimeMillis() + delay));

        log.info("Scheduled status update for order {} to {} at {}", order.getOrderKey(), nextStatus, updateTime);
    }
//    @PostConstruct
//    public void scheduleInitialOrderStatusUpdates() {
//        List<OrderEntity> orders = orderRepository.findByOrderStatus("ORDER_CREATE");
//        for (OrderEntity order : orders) {
//            scheduleNextStatusUpdate(order, "ON_DELIVERY", 1);
//        }
//    }
//
//    1분짜리 테스트
//    public void scheduleNextStatusUpdate(OrderEntity order, String nextStatus, int minutes) {
//        LocalDateTime updateTime = order.getOrderAt().plusMinutes(minutes);
//        long delay = java.time.Duration.between(LocalDateTime.now(), updateTime).toMillis();
//
//        taskScheduler.schedule(() -> {
//            updateOrderStatus(order.getOrderKey(), nextStatus);
//        }, new java.util.Date(System.currentTimeMillis() + delay));
//
//        log.info("Scheduled status update for order {} to {} at {}", order.getOrderKey(), nextStatus, updateTime);
//    }

    @Transactional
    public void updateOrderStatus(Integer orderKey, String status) {
        OrderEntity order = orderRepository.findById(orderKey).orElseThrow(
                () -> new IllegalArgumentException("Order not found")
        );

        switch (status) {
            case "ON_DELIVERY":
                if ("ORDER_CREATE".equals(order.getOrderStatus())) {
                    order.setOrderStatus("ON_DELIVERY");
                    scheduleNextStatusUpdate(order, "DELIVERY_COMPLETE", 24);
                }
                break;
            case "DELIVERY_COMPLETE":
                if ("ON_DELIVERY".equals(order.getOrderStatus())) {
                    order.setOrderStatus("DELIVERY_COMPLETE");
                    scheduleNextStatusUpdate(order, "ORDER_COMPLETE", 24);
                }
                break;
            case "ORDER_COMPLETE":
                if ("DELIVERY_COMPLETE".equals(order.getOrderStatus())) {
                    order.setOrderStatus("ORDER_COMPLETE");
                }
                break;
            default:
                log.warn("Unknown status: {}", status);
        }

        orderRepository.save(order);
        log.info("Order {} status updated to {}", order.getOrderKey(), status);
    }
}