package com.adi.ratelimiter.config;

import com.adi.ratelimiter.service.MainService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@WebFilter
public class RateLimitFilter extends OncePerRequestFilter {

    private final MainService mainService;

    public RateLimitFilter(MainService mainService) {
        this.mainService = mainService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        List<Long> l = mainService.rates.get(ipAddress);

        if (l != null && l.size() == 100) {
            if (new Date().getTime() - l.getFirst() < 60) {
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
                return;
            }

            l.clear();
        }

        if (l == null) {
            mainService.rates.put(ipAddress, new ArrayList<>());
            mainService.rates.get(ipAddress).add(new Date().getTime());
        } else {
            l.add(new Date().getTime());
        }

        filterChain.doFilter(request, response);
    }
}
