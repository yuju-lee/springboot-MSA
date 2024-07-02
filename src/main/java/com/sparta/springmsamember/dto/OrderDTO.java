package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderDTO {
    private Integer orderKey;
    private LocalDateTime orderAt;
    private String orderStatus;
}