package com.sparta.springmsaorder.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class OrderResponseDTO {
    private Integer orderKey;
    private String orderStatus;
    private LocalDateTime orderAt;
    private String message;

    public OrderResponseDTO(String message) {
        this.message = message;
    }

}