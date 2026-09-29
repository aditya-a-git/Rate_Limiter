package com.adi.ratelimiter;

public class RateLimiterUnavailableException extends RuntimeException {
    public RateLimiterUnavailableException(Throwable cause) {
        super("Rate Limiter is currently Unavailable", cause);
    }
}
