package com.adi.ratelimiter.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private final Map<String, RateLimitEntry> rates = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 100;
    private static final int WINDOW_SIZE_MS = 60_000;

    public boolean isAllowed(String ipAddress) {
//        RateLimitEntry l = rates.get(ipAddress);
        AtomicBoolean allowed = new AtomicBoolean(true);
        long currTime = System.currentTimeMillis();

        rates.compute(ipAddress, (key, currentEntry) -> {
            if (currentEntry == null) {
                return new RateLimitEntry(1, currTime);
            }

            if (currentEntry.requestCount == MAX_REQUESTS) {
                if (currTime - currentEntry.windowStart < WINDOW_SIZE_MS) {
                    allowed.set(false);
                    return currentEntry;
                }

                currentEntry.windowStart = currTime;
                currentEntry.requestCount = 1;
            } else {
                currentEntry.requestCount++;
            }

            return currentEntry;
        });

        return allowed.get();
    }
}
