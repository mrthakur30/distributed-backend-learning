package com.mrthakur30.order_service.outbox;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mrthakur30.order_service.entity.Outbox;
import com.mrthakur30.order_service.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishEvents() {

        List<Outbox> events =
                outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (Outbox event : events) {

            try {

                kafkaTemplate.send(
                        "order-created",
                        event.getAggregateId().toString(),
                        event.getPayload()
                );

                event.setProcessed(true);

            } catch (Exception e) {
                // DO NOT mark processed
                // It will retry automatically next cycle
            }
        }
    }
}