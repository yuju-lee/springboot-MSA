package com.sparta.springmsaproduct.repository;

import com.sparta.springmsaproduct.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {



}

