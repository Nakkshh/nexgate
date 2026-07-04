package com.nexgate.gateway.admin.dto;
public record CreateTenantRequest(String name, String email, String plan, int rateLimit) {}