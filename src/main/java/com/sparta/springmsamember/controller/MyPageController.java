package com.sparta.springmsamember.controller;

import com.sparta.springmsamember.dto.MyPageResponseDTO;
import com.sparta.springmsamember.dto.OrderDTO;
import com.sparta.springmsamember.dto.WishListRequestDTO;
import com.sparta.springmsamember.service.MyPageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("member/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private static final Logger log = LoggerFactory.getLogger(MyPageService.class);

    public MyPageController(MyPageService myPageService) {
        this.myPageService = myPageService;
    }

    @GetMapping
    public ResponseEntity<MyPageResponseDTO> getWishListAndOrders(@RequestHeader("X-Authenticated-User") String email) {
        // 위시리스트와 주문 내역 요청
        myPageService.sendWishListRequest(email);
        myPageService.sendOrderRequest(email);

        // 위시리스트와 주문 내역 응답
        List<WishListRequestDTO> wishList = myPageService.getWishList();
        List<OrderDTO> orders = myPageService.getOrders();

        MyPageResponseDTO response = new MyPageResponseDTO(wishList, orders);
        return ResponseEntity.ok(response);
    }
}