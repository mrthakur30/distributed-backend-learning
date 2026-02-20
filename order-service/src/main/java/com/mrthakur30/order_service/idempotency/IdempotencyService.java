package com.mrthakur30.order_service.idempotency;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class IdempotencyService {
    private final StringRedisTemplate redisTemplate;

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration RESPONSE_TTL = Duration.ofHours(24);
    
    public boolean acquireLock(String key){
        try {
            return Boolean.TRUE.equals(
                redisTemplate.opsForValue()
            .setIfAbsent("idempotency:lock"+key, "LOCK",LOCK_TTL)
            );
        } catch (Exception e) {
            return true ;
        }
    }

    public void releaseLock(String key){
        try {
            redisTemplate.delete("idempotency:lock"+key);
        } catch (Exception ignored) {}
    }

    public void storeResponse(String key, String value){
          try {
            redisTemplate.opsForValue()
                .set("idempotency:response:" + key, value, RESPONSE_TTL);
        } catch (Exception ignored) {}
    }
    
    public String getStoredResponse(String key) {
        try {
            return redisTemplate.opsForValue()
                .get("idempotency:response:" + key);
        } catch (Exception e) {
            return null; // fail-open
        }
    }
    
}
