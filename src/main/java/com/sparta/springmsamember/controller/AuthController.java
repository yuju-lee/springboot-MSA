package com.sparta.springmsamember.controller;

import com.sparta.springmsamember.dto.LoginRequestDTO;
import com.sparta.springmsamember.dto.LoginResponseDTO;
import com.sparta.springmsamember.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO, HttpServletResponse response) {
        return authService.login(loginRequestDTO, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String accessToken, @RequestHeader("X-Authenticated-User") String email) {
        authService.logout(email, accessToken);
        return ResponseEntity.ok("Logout successful");
    }
}