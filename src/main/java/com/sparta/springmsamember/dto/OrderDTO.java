package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderDTO {
    private String email;
    private String orderStatus;
    private LocalDateTime orderAt;
    private int orderNumber;

}