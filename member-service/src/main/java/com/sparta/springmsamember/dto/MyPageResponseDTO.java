package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MyPageResponseDTO {
    private List<WishListDTO> wishList;
    private List<OrderDTO> orderList;

    public MyPageResponseDTO(List<WishListDTO> wishList, List<OrderDTO> orderList) {
        this.wishList = wishList;
        this.orderList = orderList;
    }
}