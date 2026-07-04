package com.nexgate.gateway.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Day 1 smoke-test endpoint.
 * After docker-compose up, hit GET /gateway/info to verify the gateway started correctly.
 */
@RestController
public class GatewayInfoController {

    @Value("${nexgate.mock-backend-url:http://localhost:8081}")
    private String mockBackendUrl;

    @GetMapping("/gateway/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "service", "nexgate-gateway",
            "version", "1.0.0",
            "status", "UP",
            "mockBackendUrl", mockBackendUrl,
            "timestamp", Instant.now().toString(),
            "note", "Auth filter not yet active — add in Day 2"
        ));
    }
}
