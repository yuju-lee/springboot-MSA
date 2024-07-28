package com.sparta.springmsaproduct.repository;

import com.sparta.springmsaproduct.entity.WishEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.Optional;

@EnableJpaRepositories
public interface WishListRepository extends JpaRepository<WishEntity, Integer> {
    List<WishEntity> findByEmail(String email);
    Optional<WishEntity> findByProductIdAndEmail(Integer productId, String email);
}