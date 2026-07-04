package com.nexgate.gateway.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsService {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> rateLimitCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> latencyTimers = new ConcurrentHashMap<>();

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequest(String tenantId, int statusCode, long latencyMs) {
        // Request counter by tenant
        requestCounters.computeIfAbsent(tenantId, id ->
            Counter.builder("gateway_requests_total")
                .tag("tenant_id", id)
                .tag("status", statusCode < 400 ? "success" : "error")
                .description("Total requests proxied by gateway")
                .register(registry)
        ).increment();

        // Latency histogram by tenant
        latencyTimers.computeIfAbsent(tenantId, id ->
            Timer.builder("gateway_latency_seconds")
                .tag("tenant_id", id)
                .description("Gateway request latency")
                .publishPercentileHistogram()
                .register(registry)
        ).record(Duration.ofMillis(latencyMs));
    }

    public void recordRateLimitHit(String tenantId) {
        rateLimitCounters.computeIfAbsent(tenantId, id ->
            Counter.builder("rate_limit_hits_total")
                .tag("tenant_id", id)
                .description("Total rate limit rejections")
                .register(registry)
        ).increment();
    }
}