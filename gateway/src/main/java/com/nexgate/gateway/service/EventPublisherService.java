package com.nexgate.gateway.service;

import com.nexgate.gateway.event.RequestLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);
    private static final String TOPIC = "request-logs";

    private final KafkaTemplate<String, RequestLogEvent> kafkaTemplate;

    public EventPublisherService(KafkaTemplate<String, RequestLogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    public void publishRequestLog(RequestLogEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getTenantId(), event);
        } catch (Exception e) {
            log.error("Failed to publish request log event for tenant {}: {}", event.getTenantId(), e.getMessage());
        }
    }
}