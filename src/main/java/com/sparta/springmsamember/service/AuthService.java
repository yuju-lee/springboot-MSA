package com.sparta.springmsamember.service;

import com.sparta.springmsamember.dto.LoginRequestDTO;
import com.sparta.springmsamember.dto.LoginResponseDTO;
import com.sparta.springmsamember.entity.MemberEntity;
import com.sparta.springmsamember.repository.JpaMemberRepository;
import com.sparta.springmsamember.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JpaMemberRepository jpaMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final JwtUtil jwtUtil; // JWT 생성 및 검증 유틸리티
    private final TokenService tokenService; // Refresh Token 저장 및 관리 서비스

    @Autowired
    public AuthService(JpaMemberRepository jpaMemberRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, TokenService tokenService) {
        this.jpaMemberRepository = jpaMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
    }

    public ResponseEntity<LoginResponseDTO> login(LoginRequestDTO requestDto, HttpServletResponse res) {
        String email = requestDto.getEmail();
        String password = requestDto.getPassword();
        log.info(email);

        try {
            // 이메일로 회원 조회
            MemberEntity memberEntity = jpaMemberRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Not registered - Please try again"));

            // 비밀번호 검증
            if (!passwordEncoder.matches(password, memberEntity.getPassword())) {
                throw new IllegalArgumentException("Invalid password.");
            }

            // Access Token 및 Refresh Token 생성
            String accessToken = jwtUtil.createAccessToken(memberEntity.getEmail(), memberEntity.getRole());
            String refreshToken = jwtUtil.createRefreshToken();

            // Refresh Token 저장 (Redis에 저장)
            tokenService.storeRefreshToken(memberEntity.getEmail(), refreshToken);

            // Refresh Token을 응답 헤더에 추가
            res.addHeader("RefreshToken", refreshToken);
            res.addHeader("AccessToken", accessToken);

            // 이메일 정보를 응답 헤더에 추가
            res.addHeader("X-Authenticated-User", memberEntity.getEmail());

            // 로그인 성공 응답에 발급받은 토큰들 추가
            LoginResponseDTO responseDTO = new LoginResponseDTO(accessToken, refreshToken, memberEntity.getEmail());
            responseDTO.setAccessToken(accessToken);
            responseDTO.setRefreshToken(refreshToken);
            responseDTO.setMessage("Login successful! Welcome, " + memberEntity.getEmail() + "!");

            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException e) {
            log.error("Error during login: {}", e.getMessage());
            LoginResponseDTO responseDTO = new LoginResponseDTO(null, null, e.getMessage());
            responseDTO.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        }
    }

    public void logout(String email, String token) {
        if (jwtUtil.validateTokenConsideringBlacklist(token)) {
            tokenService.addToBlacklist(token);
        } else {
            throw new IllegalArgumentException("Invalid or expired JWT token.");
        }
    }
}