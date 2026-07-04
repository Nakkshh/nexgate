package com.nexgate.consumer.listener;

import com.nexgate.consumer.event.RequestLogEvent;
import com.nexgate.consumer.service.MetricsAggregatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

@Component
public class RequestLogListener {

    private static final Logger log = LoggerFactory.getLogger(RequestLogListener.class);

    private final MetricsAggregatorService aggregatorService;

    public RequestLogListener(MetricsAggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }

    @KafkaListener(
        topics = "request-logs",
        groupId = "nexgate-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(RequestLogEvent event) {
        log.info("Received log event: tenant={} status={} latency={}ms",
                event.getTenantId(), event.getStatusCode(), event.getLatencyMs());
        aggregatorService.aggregate(event);
    }

    @KafkaListener(
        topics = "request-logs-DLT",
        groupId = "nexgate-dlt-group"
    )
    public void consumeDlt(RequestLogEvent event) {
        log.error("DLT event received — permanent failure for tenant={} path={}",
                event.getTenantId(), event.getPath());
    }
}