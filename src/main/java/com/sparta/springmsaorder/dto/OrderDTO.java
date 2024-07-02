package com.sparta.springmsaorder.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderDTO {
    private Integer orderKey;
    private LocalDateTime orderAt;
    private String orderStatus;

    public OrderDTO(Integer orderKey, LocalDateTime orderAt, String orderStatus) {
        this.orderKey = orderKey;
        this.orderAt = orderAt;
        this.orderStatus = orderStatus;
    }

}
