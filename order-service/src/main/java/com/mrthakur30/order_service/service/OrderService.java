package com.mrthakur30.order_service.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import com.mrthakur30.order_service.dto.CreateOrderRequest;
import com.mrthakur30.order_service.entity.Order;
import com.mrthakur30.order_service.entity.OrderItem;
import com.mrthakur30.order_service.enums.OrderStatus;
import com.mrthakur30.order_service.repository.OrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration RESPONSE_TTL = Duration.ofHours(24);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Transactional
    public Order createOrder(CreateOrderRequest request, String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key required");
        }

        String responseKey = "idempotency:response:" + idempotencyKey;
        String lockKey = "idempotency:lock:" + idempotencyKey;

        try {
            // 1️⃣ Check cached response first
            String cachedResponse = redisTemplate.opsForValue().get(responseKey);
            if (cachedResponse != null) {
                return objectMapper.readValue(cachedResponse, Order.class);
            }
        } catch (Exception ignored) {
            // Fail-open: Redis unavailable → continue normally
        }

        boolean lockAcquired = true;

        try {
            // 2️⃣ Try acquiring distributed lock
            lockAcquired = Boolean.TRUE.equals(
                    redisTemplate.opsForValue()
                            .setIfAbsent(lockKey, "LOCK", LOCK_TTL)
            );
        } catch (Exception ignored) {
            // Fail-open strategy
        }

        if (!lockAcquired) {
            throw new RuntimeException("Duplicate request in progress");
        }

        try {
            // 3️⃣ Build Order
            Order order = Order.builder()
                    .userId(request.getUserId())
                    .status(OrderStatus.CREATED)
                    .createdAt(LocalDateTime.now())
                    .build();

            List<OrderItem> items = request.getItems().stream()
                    .map(item -> OrderItem.builder()
                            .order(order)
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .toList();

            order.setItems(items);

            // 4️⃣ Persist to DB FIRST
            orderRepository.save(order);

            // 5️⃣ Store idempotent response AFTER successful save
            try {
                redisTemplate.opsForValue()
                        .set(responseKey,
                                objectMapper.writeValueAsString(order),
                                RESPONSE_TTL);
            } catch (Exception ignored) {
                // Redis optional
            }

            return order;

        } catch (Exception e) {
            throw new RuntimeException("Order creation failed", e);
        } finally {
            try {
                redisTemplate.delete(lockKey);
            } catch (Exception ignored) {
            }
        }
    }

    public Order getOrder(Long id) {

        String cacheKey = "order:" + id;

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, Order.class);
            }
        } catch (Exception ignored) {
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        try {
            redisTemplate.opsForValue()
                    .set(cacheKey,
                            objectMapper.writeValueAsString(order),
                            CACHE_TTL);
        } catch (Exception ignored) {
        }

        return order;
    }
}