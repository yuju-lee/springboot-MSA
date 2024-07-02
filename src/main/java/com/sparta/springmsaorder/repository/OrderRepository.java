package com.sparta.springmsaorder.repository;

import com.sparta.springmsaorder.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
    List<OrderEntity> findByEmail(String email);
    List<OrderEntity> findByOrderStatus(String orderCreate);
}