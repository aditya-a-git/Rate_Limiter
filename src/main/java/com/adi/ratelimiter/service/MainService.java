package com.adi.ratelimiter.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MainService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;
    private static final int MAX_REQUESTS = 100;

    public MainService(StringRedisTemplate redisTemplate, DefaultRedisScript<Long> rateLimitScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
    }

    public boolean isAllowed(String ipAddress) {
        String key = "rate_limit:" + ipAddress;
        long count = redisTemplate.execute(rateLimitScript, List.of(key));
        System.out.println(
                "Server: " + System.getProperty("server.port")
                        + " | IP: " + ipAddress
                        + " | Count: " + count
                        + " | Allowed: " + (count <= MAX_REQUESTS)
        );
        return count <= MAX_REQUESTS;
    }
}


