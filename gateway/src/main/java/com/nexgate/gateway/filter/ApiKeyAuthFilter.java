package com.nexgate.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgate.gateway.model.TenantContext;
import com.nexgate.gateway.model.TenantContext.TenantInfo;
import com.nexgate.gateway.service.MetricsService;
import com.nexgate.gateway.service.RateLimiterService;
import com.nexgate.gateway.service.RateLimiterService.RateLimitResult;
import com.nexgate.gateway.service.TenantCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final Set<String> PUBLIC = Set.of("/actuator/health", "/actuator/prometheus",
            "/actuator/info", "/actuator/metrics", "/gateway/info");

    private final TenantCacheService tenantCacheService;
    private final RateLimiterService rateLimiterService;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(TenantCacheService tenantCacheService,
                            RateLimiterService rateLimiterService,
                            MetricsService metricsService,
                            ObjectMapper objectMapper) {
        this.tenantCacheService = tenantCacheService;
        this.rateLimiterService = rateLimiterService;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (PUBLIC.contains(path) || path.startsWith("/actuator/") || path.startsWith("/admin/")) {
            chain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(API_KEY_HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            sendError(response, 401, "missing_api_key", "X-API-Key header is required", null);
            return;
        }

        Optional<TenantInfo> tenantOpt = tenantCacheService.resolveTenant(rawKey);
        if (tenantOpt.isEmpty()) {
            sendError(response, 401, "invalid_api_key", "API key is invalid or revoked", null);
            return;
        }

        TenantInfo tenant = tenantOpt.get();

        RateLimitResult rl = rateLimiterService.checkLimit(tenant.tenantId(), tenant.rateLimit());
        response.setHeader("X-RateLimit-Limit", String.valueOf(tenant.rateLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, rl.remaining())));

        if (!rl.allowed()) {
            response.setHeader("Retry-After", "60");
            log.info("Rate limit exceeded for tenant {}", tenant.tenantName());
            metricsService.recordRateLimitHit(tenant.tenantId());
            sendError(response, 429, "rate_limit_exceeded",
                    "Rate limit exceeded. Try again later.", 60);
            return;
        }

        TenantContext.set(tenant);
        try {
            chain.doFilter(new TenantHeaderRequestWrapper(request, tenant.tenantId()), response);
        } finally {
            TenantContext.clear();
        }
    }

    private void sendError(HttpServletResponse res, int status, String error, String msg, Integer retryAfter)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        var body = new java.util.HashMap<String, Object>();
        body.put("error", error);
        body.put("message", msg);
        body.put("status", status);
        body.put("timestamp", Instant.now().toString());
        if (retryAfter != null) body.put("retryAfter", retryAfter);
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }
}