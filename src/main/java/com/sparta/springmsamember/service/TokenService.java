package com.sparta.springmsamember.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeRefreshToken(String email, String refreshToken) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(email + ":refreshToken", refreshToken, Duration.ofDays(7)); // Refresh token expiration set to 7 days
    }

    public String getRefreshToken(String email) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return ops.get(email + ":refreshToken");
    }

    public void addToBlacklist(String token) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(token, "blacklisted", Duration.ofHours(1)); // Blacklist expiration time same as token expiration
    }

    public boolean isBlacklisted(String token) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return "blacklisted".equals(ops.get(token));
    }

    public void storeAccessToken(String email, String accessToken) {
        redisTemplate.opsForSet().add(email + ":accessTokens", accessToken);
    }

    public Set<String> getAccessTokens(String email) {
        return redisTemplate.opsForSet().members(email + ":accessTokens");
    }

    public void removeAccessToken(String email, String accessToken) {
        redisTemplate.opsForSet().remove(email + ":accessTokens", accessToken);
    }

    // 모든 기기에서 로그아웃
    public void logoutFromAllDevices(String email) {
        // 리프레시 토큰을 제거
        redisTemplate.delete(email + ":refreshToken");

        // 모든 액세스 토큰을 블랙리스트에 추가
        Set<String> accessTokens = getAccessTokens(email);
        if (accessTokens != null) {
            for (String token : accessTokens) {
                addToBlacklist(token);
            }
            // 사용자의 모든 액세스 토큰 목록을 삭제
            redisTemplate.delete(email + ":accessTokens");
        }
    }
}