package com.adi.ratelimiter.config;

import com.adi.ratelimiter.service.MainService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final MainService mainService;

    private final MeterRegistry meterRegistry;
    private final Timer checkDuration;
    private final AtomicInteger actReq = new AtomicInteger();

    public RateLimitFilter(MainService mainService, MeterRegistry meterRegistry) {
        this.mainService = mainService;

        this.meterRegistry = meterRegistry;
        this.checkDuration = Timer.builder("rate_limit_check_duration").register(meterRegistry);
        Gauge.builder("rate-limit-active-requests", actReq, AtomicInteger::get).register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ipAddress = request.getRemoteAddr();

        actReq.incrementAndGet();
        Timer.Sample checkSample = Timer.start(meterRegistry);

        try {
            if (!mainService.isAllowed(ipAddress, response)) {
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            checkSample.stop(checkDuration);
            actReq.decrementAndGet();
        }


    }
}
