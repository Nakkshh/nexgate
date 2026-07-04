package com.nexgate.consumer.controller;

import com.nexgate.consumer.model.TenantMetrics;
import com.nexgate.consumer.repository.TenantMetricsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final TenantMetricsRepository repository;

    public MetricsController(TenantMetricsRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/history/{tenantId}")
    public ResponseEntity<List<TenantMetrics>> getHistory(@PathVariable String tenantId,
                                                           @RequestParam(defaultValue = "60") int minutes) {
        try {
            UUID tenantUuid = UUID.fromString(tenantId);
            Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);
            List<TenantMetrics> history = repository.findByTenantIdSince(tenantUuid, since);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<TenantMetrics>> getAll() {
        Instant since = Instant.now().minus(60, ChronoUnit.MINUTES);
        return ResponseEntity.ok(repository.findAllSince(since));
    }
}