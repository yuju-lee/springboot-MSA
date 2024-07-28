package com.sparta.springmsaproduct.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore // JSON 응답에서 productId 필드를 무시
    private Integer productId;

    private String productName;
    private Integer price;
    private Integer quantity;
    private Integer likeCount = 0;

}