package com.mrthakur30.order_service.filter;

import java.io.IOException;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitFilter {
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);

  
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");

        if (userId != null) {
            try {
                String key = "rate_limit:" + userId;

                Long count = redisTemplate.opsForValue().increment(key);

                if (count != null && count == 1) {
                    redisTemplate.expire(key, WINDOW);
                }

                if (count != null && count > MAX_REQUESTS) {
                    response.setStatus(429);
                    response.getWriter().write("Too many requests");
                    return;
                }
            } catch (Exception ignored) {
                // Fail-open
            }
        }

        filterChain.doFilter(request, response);
    }

}
