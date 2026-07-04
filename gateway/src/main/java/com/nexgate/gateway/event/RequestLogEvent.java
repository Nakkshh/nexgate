package com.nexgate.gateway.event;

import java.time.Instant;

public class RequestLogEvent {
    private String tenantId;
    private String apiKey;
    private String method;
    private String path;
    private int statusCode;
    private long latencyMs;
    private Instant timestamp;

    public RequestLogEvent() {}

    public RequestLogEvent(String tenantId, String apiKey, String method, String path, int statusCode, long latencyMs) {
        this.tenantId = tenantId;
        this.apiKey = apiKey;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}