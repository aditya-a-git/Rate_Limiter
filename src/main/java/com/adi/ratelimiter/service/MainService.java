package com.adi.ratelimiter.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MainService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List<Long>> rateLimitScript;
    private static final int MAX_REQUESTS = 100;

    public MainService(StringRedisTemplate redisTemplate, DefaultRedisScript<List<Long>> rateLimitScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
    }

    public boolean isAllowed(String ipAddress, HttpServletResponse response) {
        String key = "rate_limit:" + ipAddress;
        List<Long> redisResponse = redisTemplate.execute(rateLimitScript, List.of(key));

        response.addHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS - redisResponse.getFirst()));
        response.addHeader("X-RateLimit-Reset-After", String.valueOf(redisResponse.getLast()));

        return redisResponse.getFirst() <= MAX_REQUESTS;
    }
}


