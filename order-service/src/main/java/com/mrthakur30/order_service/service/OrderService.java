package com.mrthakur30.order_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mrthakur30.order_service.dto.CreateOrderRequest;
import com.mrthakur30.order_service.entity.Order;
import com.mrthakur30.order_service.entity.OrderItem;
import com.mrthakur30.order_service.enums.OrderStatus;
import com.mrthakur30.order_service.repository.OrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {

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

        return orderRepository.save(order);
    }


    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

}
