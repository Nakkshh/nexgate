package com.nexgate.consumer.service;

import com.nexgate.consumer.model.TenantMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetricsPushService {

    private static final Logger log = LoggerFactory.getLogger(MetricsPushService.class);
    private static final double ERROR_RATE_THRESHOLD = 0.1; // 10%

    private final SimpMessagingTemplate messagingTemplate;

    public MetricsPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void pushMetrics(TenantMetrics metrics) {
        String destination = "/topic/metrics/" + metrics.getTenantId();
        messagingTemplate.convertAndSend(destination, metrics);
        checkErrorRate(metrics);
    }

    private void checkErrorRate(TenantMetrics metrics) {
        if (metrics.getReqCount() == 0) return;
        double errorRate = (double) metrics.getErrorCount() / metrics.getReqCount();
        if (errorRate > ERROR_RATE_THRESHOLD) {
            log.warn("ALERT: tenant={} error_rate={:.1f}% ({}/{} requests)",
                    metrics.getTenantId(),
                    errorRate * 100,
                    metrics.getErrorCount(),
                    metrics.getReqCount());
        }
    }
}