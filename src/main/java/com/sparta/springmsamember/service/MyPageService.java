package com.sparta.springmsamember.service;

import com.sparta.springmsamember.client.OrderClient;
import com.sparta.springmsamember.client.ProductClient;
import com.sparta.springmsamember.dto.MyPageResponseDTO;
import com.sparta.springmsamember.dto.OrderDTO;
import com.sparta.springmsamember.dto.WishListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MyPageService {

    private final ProductClient productClient;
    private final OrderClient orderClient;

    public MyPageService(ProductClient productClient, OrderClient orderClient) {
        this.productClient = productClient;
        this.orderClient = orderClient;
    }

    public MyPageResponseDTO getMyPageData(String email) {
        List<WishListDTO> wishList = productClient.getWishList(email);
        List<OrderDTO> orderList = orderClient.getOrders(email);

        return new MyPageResponseDTO(wishList, orderList);
    }
}