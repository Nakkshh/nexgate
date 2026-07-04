package com.nexgate.mock.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a real partner API sitting behind NexGate.
 * All endpoints return realistic-looking dummy data.
 * Random latency (10–120ms) simulates real-world response times
 * so gateway latency measurements are meaningful during load tests.
 */
@RestController
@RequestMapping
public class MockApiController {

    private static final Logger log = LoggerFactory.getLogger(MockApiController.class);

    // ── Products ─────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        simulateLatency(10, 60);
        log.info("GET /products page={} size={}", page, size);

        List<Map<String, Object>> products = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", UUID.randomUUID().toString());
            p.put("name", "Product " + ((page - 1) * size + i));
            p.put("price", Math.round(ThreadLocalRandom.current().nextDouble(9.99, 999.99) * 100.0) / 100.0);
            p.put("stock", ThreadLocalRandom.current().nextInt(0, 500));
            p.put("category", randomChoice("electronics", "clothing", "books", "home", "sports"));
            products.add(p);
        }

        return ResponseEntity.ok(Map.of(
                "data", products,
                "page", page,
                "size", size,
                "total", 1243,
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String id) {
        simulateLatency(10, 40);
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", id);
        product.put("name", "Product " + id.substring(0, 8));
        product.put("price", 49.99);
        product.put("stock", ThreadLocalRandom.current().nextInt(0, 200));
        product.put("description", "High quality product with excellent features.");
        product.put("category", "electronics");
        product.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(product);
    }

    // ── Orders ───────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        simulateLatency(20, 80);
        log.info("GET /orders tenant={}", tenantId);

        List<Map<String, Object>> orders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", "ORD-" + ThreadLocalRandom.current().nextInt(10000, 99999));
            o.put("status", randomChoice("pending", "processing", "shipped", "delivered", "cancelled"));
            o.put("total", Math.round(ThreadLocalRandom.current().nextDouble(10.0, 2000.0) * 100.0) / 100.0);
            o.put("items", ThreadLocalRandom.current().nextInt(1, 8));
            o.put("createdAt", Instant.now().minusSeconds(ThreadLocalRandom.current().nextInt(0, 86400)).toString());
            orders.add(o);
        }

        return ResponseEntity.ok(Map.of(
                "data", orders,
                "page", page,
                "total", 387,
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        simulateLatency(30, 120);
        log.info("POST /orders body={}", body);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", "ORD-" + ThreadLocalRandom.current().nextInt(10000, 99999));
        response.put("status", "pending");
        response.put("items", body.getOrDefault("items", List.of()));
        response.put("total", body.getOrDefault("total", 0.0));
        response.put("estimatedDelivery", Instant.now().plusSeconds(259200).toString()); // 3 days
        response.put("createdAt", Instant.now().toString());

        return ResponseEntity.status(201).body(response);
    }

    // ── Users ────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(@RequestParam(defaultValue = "1") int page) {
        simulateLatency(15, 60);

        List<Map<String, Object>> users = new ArrayList<>();
        String[] firstNames = {"Alice", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace", "Heidi"};
        String[] lastNames  = {"Smith", "Jones", "Williams", "Brown", "Davis", "Miller", "Wilson"};

        for (int i = 0; i < 10; i++) {
            Map<String, Object> u = new LinkedHashMap<>();
            String first = firstNames[ThreadLocalRandom.current().nextInt(firstNames.length)];
            String last  = lastNames[ThreadLocalRandom.current().nextInt(lastNames.length)];
            u.put("id", UUID.randomUUID().toString());
            u.put("name", first + " " + last);
            u.put("email", first.toLowerCase() + "." + last.toLowerCase() + "@example.com");
            u.put("role", randomChoice("admin", "user", "viewer"));
            u.put("active", ThreadLocalRandom.current().nextBoolean());
            users.add(u);
        }

        return ResponseEntity.ok(Map.of(
                "data", users,
                "page", page,
                "total", 5821,
                "timestamp", Instant.now().toString()
        ));
    }

    // ── Payments ─────────────────────────────────────────────────

    @PostMapping("/payments")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> body) {
        simulateLatency(50, 120); // Payments are slower — realistic
        log.info("POST /payments amount={}", body.get("amount"));

        // Simulate ~5% payment failure for realistic error rates
        if (ThreadLocalRandom.current().nextInt(100) < 5) {
            return ResponseEntity.status(402).body(Map.of(
                    "error", "payment_failed",
                    "message", "Card declined by issuing bank",
                    "timestamp", Instant.now().toString()
            ));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        response.put("status", "success");
        response.put("amount", body.getOrDefault("amount", 0));
        response.put("currency", body.getOrDefault("currency", "INR"));
        response.put("method", body.getOrDefault("method", "card"));
        response.put("processedAt", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    // ── Health ───────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "mock-backend",
                "timestamp", Instant.now().toString()
        ));
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SafeVarargs
    private <T> T randomChoice(T... options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }
}
