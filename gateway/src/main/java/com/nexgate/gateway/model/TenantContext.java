package com.nexgate.gateway.model;

/**
 * Thread-local holder for the authenticated tenant on each request.
 * Set by ApiKeyAuthFilter after successful auth, read by proxy and Kafka producer.
 * Cleared at the end of every request to prevent leaks across thread pool reuse.
 */
public class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    public static void set(TenantInfo info) {
        CURRENT.set(info);
    }

    public static TenantInfo get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Lightweight record carrying everything the gateway needs per request.
     * Avoids repeated DB/Redis lookups within the same request.
     */
    public record TenantInfo(
            String tenantId,
            String tenantName,
            String apiKeyId,
            String plan,
            int rateLimit
    ) {}
}
