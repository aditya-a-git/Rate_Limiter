package com.adi.ratelimiter.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class RateLimitEntry {
    long windowStart;
    int requestCount = 1;
}

@Service
public class MainService {
    private final Map<String, RateLimitEntry> rates = new ConcurrentHashMap<>();

    public boolean isAllowed(String ipAddress) {
        RateLimitEntry l = rates.get(ipAddress);

        if (l != null) {
            if (l.requestCount == 100) {
                if (new Date().getTime() - l.windowStart < 60_000) {
                    return false;
                }

                l.requestCount = 1;
                l.windowStart = new Date().getTime();
            } else {
                l.requestCount++;
            }
        }

        if (l == null) {
            rates.put(ipAddress, new RateLimitEntry());
            rates.get(ipAddress).windowStart = new Date().getTime();
        }

        return true;
    }
}
