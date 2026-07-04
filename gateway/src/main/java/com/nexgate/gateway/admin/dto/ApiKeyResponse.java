package com.nexgate.gateway.admin.dto;
public record ApiKeyResponse(String keyPrefix, String rawKey, String tenantId, String tenantName, String message) {}