package com.sparta.springmsaorder.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orderdetail")
@NoArgsConstructor
@Getter
@Setter
public class OrderDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderDetailKey;

    private Integer orderKey;
    private Integer productID;
    private Integer orderPrice;
    private Integer productCount;
}