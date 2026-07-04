package com.nexgate.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final String KEY_PREFIX = "ratelimit:";

    @Value("${nexgate.rate-limit.window-seconds:60}")
    private long windowSeconds;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;

    public RateLimiterService(StringRedisTemplate redis) throws IOException {
        this.redis = redis;
        this.script = new DefaultRedisScript<>();
        this.script.setResultType(List.class);
        String lua = new String(
                new ClassPathResource("scripts/rate_limiter.lua").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        this.script.setScriptText(lua);
    }

    public record RateLimitResult(boolean allowed, long remaining) {}

    /**
     * Checks and records a request against the tenant's rate limit.
     * Atomic via Lua — safe across multiple gateway instances.
     */
    public RateLimitResult checkLimit(String tenantId, int limit) {
        String key = KEY_PREFIX + tenantId;
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000;

        try {
            @SuppressWarnings("unchecked")
            List<Long> result = redis.execute(script,
                    List.of(key),
                    String.valueOf(now), String.valueOf(windowMs), String.valueOf(limit));

            if (result == null || result.size() < 2) {
                // Redis failed — fail open (allow request) rather than blocking all traffic
                log.warn("Rate limit script returned null/invalid for tenant {}", tenantId);
                return new RateLimitResult(true, limit);
            }

            boolean allowed = result.get(0) == 1L;
            long remaining = result.get(1);
            return new RateLimitResult(allowed, remaining);

        } catch (Exception e) {
            log.error("Rate limit check failed for tenant {}: {}", tenantId, e.getMessage());
            // Fail open — never let Redis downtime take down the whole gateway
            return new RateLimitResult(true, limit);
        }
    }
}