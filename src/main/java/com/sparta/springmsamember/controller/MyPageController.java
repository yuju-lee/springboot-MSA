package com.sparta.springmsamember.controller;

import com.sparta.springmsamember.dto.MyPageResponseDTO;
import com.sparta.springmsamember.service.MyPageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MyPageController {

    private final MyPageService myPageService;
    private static final Logger log = LoggerFactory.getLogger(MyPageService.class);

    public MyPageController(MyPageService myPageService) {
        this.myPageService = myPageService;
    }

    @GetMapping("/mypage")
    public ResponseEntity<MyPageResponseDTO> getMyPageData(@RequestHeader("X-Authenticated-User") String email) {
        MyPageResponseDTO myPageData = myPageService.getMyPageData(email);
        return ResponseEntity.ok(myPageData);
    }
}