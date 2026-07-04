package com.nexgate.gateway.repository;

import com.nexgate.gateway.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    // Hot path — called on EVERY gateway request
    // Uses idx_api_keys_key_hash index for O(1) lookup
    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByTenantId(UUID tenantId);

    @Modifying
    @Transactional
    @Query("UPDATE ApiKey k SET k.lastUsedAt = :now WHERE k.id = :id")
    void updateLastUsedAt(UUID id, Instant now);
}
