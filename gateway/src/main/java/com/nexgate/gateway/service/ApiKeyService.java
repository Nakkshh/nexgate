package com.nexgate.gateway.service;

import com.nexgate.gateway.model.ApiKey;
import com.nexgate.gateway.model.Tenant;
import com.nexgate.gateway.repository.ApiKeyRepository;
import com.nexgate.gateway.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, TenantRepository tenantRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ApiKey> findValidApiKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return Optional.empty();
        String hash = sha256(rawKey);
        Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyHash(hash);
        if (keyOpt.isEmpty()) return Optional.empty();
        ApiKey key = keyOpt.get();
        if (!key.isActive() || key.isExpired() || !key.getTenant().isActive()) return Optional.empty();
        return Optional.of(key);
    }

    @Transactional
    public String generateApiKey(UUID tenantId, String keyName) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        String rawKey = "nxg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 28);
        ApiKey apiKey = new ApiKey();
        apiKey.setTenant(tenant);
        apiKey.setKeyHash(sha256(rawKey));
        apiKey.setKeyPrefix(rawKey.substring(0, 8));
        apiKey.setName(keyName);
        apiKey.setActive(true);
        apiKeyRepository.save(apiKey);
        log.info("Generated API key {} for tenant {}", rawKey.substring(0, 8), tenant.getName());
        return rawKey;
    }

    @Async
    public void updateLastUsed(UUID apiKeyId) {
        try {
            apiKeyRepository.updateLastUsedAt(apiKeyId, Instant.now());
        } catch (Exception e) {
            log.warn("Failed to update last_used_at: {}", e.getMessage());
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}