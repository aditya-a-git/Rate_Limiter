package com.adi.ratelimiter.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MainService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> rateLimitScript;
    private static final int MAX_REQUESTS = 100;

    private final MeterRegistry meterRegistry;
    private final Counter requestsTotal;
    private final Counter allowedTotal;
    private final Counter rejectedTotal;
    private final Counter redisErrors;
    private final Timer redisLatency;

    public MainService(StringRedisTemplate redisTemplate,
                       DefaultRedisScript<List> rateLimitScript,
                       MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;

        this.meterRegistry = meterRegistry;
        this.requestsTotal = Counter.builder("rate_limit_requests_total").register(meterRegistry);
        this.allowedTotal = Counter.builder("rate_limit_allowed_total").register(meterRegistry);
        this.rejectedTotal = Counter.builder("rate_limit_rejected_total").register(meterRegistry);
        this.redisErrors = Counter.builder("rate_limit_redis_errors").register(meterRegistry);
        this.redisLatency = Timer.builder("rate_limit_redis_latency").register(meterRegistry);
    }

    public boolean isAllowed(String ipAddress, HttpServletResponse response) {
        String key = "rate_limit:" + ipAddress;
        List<Long> redisResponse;

        Timer.Sample redisSample = Timer.start(meterRegistry);

        try {
            redisResponse = (List<Long>) redisTemplate.execute(rateLimitScript, List.of(key));
        } catch (RuntimeException e) {
            redisErrors.increment();
            throw e;
        } finally {
            redisSample.stop(redisLatency);
        }

        response.addHeader("RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        response.addHeader("RateLimit-Remaining", String.valueOf(MAX_REQUESTS - redisResponse.getFirst()));
        response.addHeader("RateLimit-Reset-After", String.valueOf(redisResponse.getLast()));
        requestsTotal.increment();

        if (redisResponse.getFirst() <= MAX_REQUESTS) {
            allowedTotal.increment();
            return true;
        }

        rejectedTotal.increment();
        return false;
    }
}


