package com.sparta.springmsamember.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeRefreshToken(String email, String refreshToken) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(email, refreshToken, Duration.ofDays(7)); // Refresh token expiration set to 7 days
    }

    public String getRefreshToken(String email) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return ops.get(email);
    }

    public void addToBlacklist(String token) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(token, "blacklisted", Duration.ofHours(1)); // Blacklist expiration time same as token expiration
    }

    public boolean isBlacklisted(String token) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return "blacklisted".equals(ops.get(token));
    }
}