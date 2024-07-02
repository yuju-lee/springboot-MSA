package com.sparta.springmsaorder.repository;

import com.sparta.springmsaorder.entity.OrderDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetailEntity, Integer> {
    List<OrderDetailEntity> findByOrderKey(Integer orderKey);
    void deleteByOrderKey(Integer orderKey);
}