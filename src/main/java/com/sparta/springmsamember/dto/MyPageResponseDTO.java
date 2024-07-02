package com.sparta.springmsamember.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class MyPageResponseDTO {

    private List<WishListRequestDTO> wishList;
    private List<OrderDTO> orders;

    public MyPageResponseDTO(List<WishListRequestDTO> wishList, List<OrderDTO> orders) {
        this.wishList = wishList;
        this.orders = orders;
    }

    public List<WishListRequestDTO> getWishList() {
        return wishList;
    }

    public void setWishList(List<WishListRequestDTO> wishList) {
        this.wishList = wishList;
    }

    public List<OrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDTO> orders) {
        this.orders = orders;
    }
}