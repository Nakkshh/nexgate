package com.nexgate.consumer.repository;

import com.nexgate.consumer.model.TenantMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantMetricsRepository extends JpaRepository<TenantMetrics, UUID> {

    @Query("SELECT tm FROM TenantMetrics tm WHERE tm.tenantId = :tenantId AND tm.windowStart = :windowStart")
    Optional<TenantMetrics> findByTenantIdAndWindowStart(
            @Param("tenantId") UUID tenantId,
            @Param("windowStart") Instant windowStart);

    @Query("SELECT tm FROM TenantMetrics tm WHERE tm.tenantId = :tenantId AND tm.windowStart >= :since ORDER BY tm.windowStart DESC")
    List<TenantMetrics> findByTenantIdSince(
            @Param("tenantId") UUID tenantId,
            @Param("since") Instant since);

    @Query("SELECT tm FROM TenantMetrics tm WHERE tm.windowStart >= :since ORDER BY tm.windowStart DESC")
    List<TenantMetrics> findAllSince(@Param("since") Instant since);
}