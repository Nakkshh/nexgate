package com.nexgate.gateway.admin;

import com.nexgate.gateway.admin.dto.*;
import com.nexgate.gateway.model.Tenant;
import com.nexgate.gateway.repository.TenantRepository;
import com.nexgate.gateway.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final TenantRepository tenantRepository;
    private final ApiKeyService apiKeyService;

    public AdminController(TenantRepository tenantRepository, ApiKeyService apiKeyService) {
        this.tenantRepository = tenantRepository;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/tenants")
    public ResponseEntity<Tenant> createTenant(@RequestBody CreateTenantRequest req) {
        Tenant t = new Tenant();
        t.setName(req.name());
        t.setEmail(req.email());
        t.setPlan(req.plan() != null ? req.plan() : "free");
        t.setRateLimit(req.rateLimit() > 0 ? req.rateLimit() : 100);
        t.setActive(true);
        return ResponseEntity.ok(tenantRepository.save(t));
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> listTenants() {
        return ResponseEntity.ok(tenantRepository.findAll());
    }

    @PostMapping("/api-keys")
    public ResponseEntity<ApiKeyResponse> createApiKey(@RequestBody CreateApiKeyRequest req) {
        UUID tenantId = UUID.fromString(req.tenantId());
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        String rawKey = apiKeyService.generateApiKey(tenantId, req.keyName());
        return ResponseEntity.ok(new ApiKeyResponse(
                rawKey.substring(0, 8), rawKey,
                tenantId.toString(), tenant.getName(),
                "Save this key — it will never be shown again"
        ));
    }
}