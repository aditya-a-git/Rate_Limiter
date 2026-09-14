package com.adi.ratelimiter.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MainService {

    final StringRedisTemplate redisTemplate;
    final DefaultRedisScript<Long> rateLimitScript;
    private static final int MAX_REQUESTS = 100;

    public MainService(StringRedisTemplate redisTemplate, DefaultRedisScript<Long> rateLimitScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
    }

    public boolean isAllowed(String ipAddress) {
        String key = "rate_limit:" + ipAddress;
        Long count = redisTemplate.execute(rateLimitScript, List.of(key));
        return count <= MAX_REQUESTS;
    }
}
