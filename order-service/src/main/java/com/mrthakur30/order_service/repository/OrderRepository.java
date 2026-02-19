package com.mrthakur30.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mrthakur30.order_service.entity.Order;


public interface OrderRepository extends JpaRepository<Order, Long> {
}
