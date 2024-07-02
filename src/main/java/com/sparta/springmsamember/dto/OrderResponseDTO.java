package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderResponseDTO {

    private Integer orderKey;
    private String orderStatus;
    private LocalDateTime orderAt;

    public OrderResponseDTO() {
    }

    public OrderResponseDTO(Integer orderKey, String orderStatus, LocalDateTime orderAt) {
        this.orderKey = orderKey;
        this.orderStatus = orderStatus;
        this.orderAt = orderAt;
    }
}