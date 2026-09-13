package com.adi.ratelimiter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class RateLimitEntry {
    long windowStart;
    int requestCount;

    public RateLimitEntry(int requestCount, long windowStart) {
        this.requestCount = requestCount;
        this.windowStart = windowStart;
    }
}

@Service
public class MainService {

    final StringRedisTemplate redisTemplate;
    //    private final Map<String, RateLimitEntry> rates = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 100;
//    private static final int WINDOW_SIZE_MS = 60_000;

    public MainService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String ipAddress) {
//        long currTime = System.currentTimeMillis();
//        AtomicBoolean allowed = new AtomicBoolean(true);
//
//        rates.compute(ipAddress, (key, currentEntry) -> {
//            if (currentEntry == null) {
//                return new RateLimitEntry(1, currTime);
//            }
//
//            if (currentEntry.requestCount == MAX_REQUESTS) {
//                if (currTime - currentEntry.windowStart < WINDOW_SIZE_MS) {
//                    allowed.set(false);
//                    return currentEntry;
//                }
//
//                currentEntry.windowStart = currTime;
//                currentEntry.requestCount = 1;
//            } else {
//                currentEntry.requestCount++;
//            }
//
//            return currentEntry;
//        });

//        return allowed.get();

        Long count = redisTemplate.opsForValue().increment(ipAddress);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(ipAddress, Duration.ofSeconds(60));
        }

        return count <= MAX_REQUESTS;
    }
}
