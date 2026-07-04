package com.nexgate.consumer.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_metrics")
public class TenantMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Column(name = "req_count", nullable = false)
    private int reqCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "avg_latency_ms", columnDefinition = "numeric(8,2)")
    private Double avgLatencyMs;

    @Column(name = "p99_latency_ms")
    private Integer p99LatencyMs;

    @Column(name = "rate_limit_hits", nullable = false)
    private int rateLimitHits;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TenantMetrics() {}

    public TenantMetrics(UUID tenantId, Instant windowStart) {
        this.tenantId = tenantId;
        this.windowStart = windowStart;
        this.windowEnd = windowStart.plusSeconds(60);
        this.reqCount = 0;
        this.errorCount = 0;
        this.avgLatencyMs = 0.0;
        this.rateLimitHits = 0;
        this.createdAt = Instant.now();
    }

    public void recordRequest(int statusCode, long latencyMs) {
        this.reqCount++;
        double total = (this.avgLatencyMs != null ? this.avgLatencyMs : 0.0) * (this.reqCount - 1) + latencyMs;
        this.avgLatencyMs = total / this.reqCount;
        if (statusCode >= 400) {
            this.errorCount++;
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Instant windowEnd) { this.windowEnd = windowEnd; }
    public int getReqCount() { return reqCount; }
    public void setReqCount(int reqCount) { this.reqCount = reqCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public Double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(Double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }
    public Integer getP99LatencyMs() { return p99LatencyMs; }
    public void setP99LatencyMs(Integer p99LatencyMs) { this.p99LatencyMs = p99LatencyMs; }
    public int getRateLimitHits() { return rateLimitHits; }
    public void setRateLimitHits(int rateLimitHits) { this.rateLimitHits = rateLimitHits; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}