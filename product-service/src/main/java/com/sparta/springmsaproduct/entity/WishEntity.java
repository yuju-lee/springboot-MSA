package com.sparta.springmsaproduct.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wishlist")
@NoArgsConstructor
@Getter
@Setter
public class WishEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Integer wishId;

    @JsonIgnore
    private Integer productId;

    private String productName;
    private Integer price;
    private Integer quantity;
    private String email;
}