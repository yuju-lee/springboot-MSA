package com.sparta.springmsamember.controller;

import com.sparta.springmsamember.dto.LoginRequestDTO;
import com.sparta.springmsamember.dto.LoginResponseDTO;
import com.sparta.springmsamember.service.AuthService;
import com.sparta.springmsamember.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
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

    @PostMapping("/logout-all")
    public ResponseEntity<String> logoutFromAllDevices(@RequestHeader("X-Authenticated-User") String email) {
        tokenService.logoutFromAllDevices(email);
        return ResponseEntity.ok("Logged out from all devices successfully.");
    }
}