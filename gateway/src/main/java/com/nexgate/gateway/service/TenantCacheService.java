package com.nexgate.gateway.service;

import com.nexgate.gateway.model.ApiKey;
import com.nexgate.gateway.model.TenantContext.TenantInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TenantCacheService {

    private static final Logger log = LoggerFactory.getLogger(TenantCacheService.class);
    private static final String PREFIX = "tenant_config:";
    private static final String D = "|";

    private final StringRedisTemplate redis;
    private final ApiKeyService apiKeyService;

    @Value("${nexgate.redis.tenant-cache-ttl:300}")
    private long ttl;

    public TenantCacheService(StringRedisTemplate redis, ApiKeyService apiKeyService) {
        this.redis = redis;
        this.apiKeyService = apiKeyService;
    }

    public Optional<TenantInfo> resolveTenant(String rawKey) {
        String cacheKey = PREFIX + ApiKeyService.sha256(rawKey);
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) return Optional.of(deserialize(cached));
        } catch (Exception e) {
            log.warn("Redis read failed, falling back to DB: {}", e.getMessage());
        }

        Optional<ApiKey> keyOpt = apiKeyService.findValidApiKey(rawKey);
        if (keyOpt.isEmpty()) return Optional.empty();

        ApiKey key = keyOpt.get();
        TenantInfo info = new TenantInfo(
                key.getTenant().getId().toString(),
                key.getTenant().getName(),
                key.getId().toString(),
                key.getTenant().getPlan(),
                key.getTenant().getRateLimit()
        );

        try {
            redis.opsForValue().set(cacheKey, serialize(info), Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("Redis write failed: {}", e.getMessage());
        }

        apiKeyService.updateLastUsed(key.getId());
        return Optional.of(info);
    }

    private String serialize(TenantInfo i) {
        return String.join(D, i.tenantId(), i.tenantName(), i.apiKeyId(), i.plan(), String.valueOf(i.rateLimit()));
    }

    private TenantInfo deserialize(String s) {
        String[] p = s.split("\\" + D, 5);
        return new TenantInfo(p[0], p[1], p[2], p[3], Integer.parseInt(p[4]));
    }
}