package com.sparta.springmsaproduct.repository;

import com.sparta.springmsaproduct.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Integer> {
    Optional<LikeEntity> findByProductIdAndEmail(Integer productId, String email);
}