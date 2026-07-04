package com.nexgate.consumer.service;

import com.nexgate.consumer.event.RequestLogEvent;
import com.nexgate.consumer.model.TenantMetrics;
import com.nexgate.consumer.repository.TenantMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class MetricsAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(MetricsAggregatorService.class);

    private final TenantMetricsRepository repository;
    private final MetricsPushService metricsPushService;

    public MetricsAggregatorService(TenantMetricsRepository repository,
                                    MetricsPushService metricsPushService) {
        this.repository = repository;
        this.metricsPushService = metricsPushService;
    }

    @Transactional
    public void aggregate(RequestLogEvent event) {
        try {
            Instant windowStart = event.getTimestamp().truncatedTo(ChronoUnit.MINUTES);

            UUID tenantId;
            try {
                tenantId = UUID.fromString(event.getTenantId());
            } catch (Exception e) {
                log.warn("Invalid tenant UUID '{}', skipping event", event.getTenantId());
                return;
            }

            TenantMetrics metrics = repository
                    .findByTenantIdAndWindowStart(tenantId, windowStart)
                    .orElseGet(() -> new TenantMetrics(tenantId, windowStart));

            metrics.recordRequest(event.getStatusCode(), event.getLatencyMs());
            TenantMetrics saved = repository.save(metrics);

            metricsPushService.pushMetrics(saved);

        } catch (Exception e) {
            log.error("Failed to aggregate metrics for tenant {}: {}", event.getTenantId(), e.getMessage());
            throw e;
        }
    }
}