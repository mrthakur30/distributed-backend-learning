package com.mrthakur30.order_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mrthakur30.order_service.entity.Outbox;

public interface  OutboxRepository extends JpaRepository<Outbox, Long > {
    List<Outbox> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
