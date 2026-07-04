package com.nexgate.gateway.proxy;

import com.nexgate.gateway.event.RequestLogEvent;
import com.nexgate.gateway.model.TenantContext;
import com.nexgate.gateway.model.TenantContext.TenantInfo;
import com.nexgate.gateway.service.EventPublisherService;
import com.nexgate.gateway.service.MetricsService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Enumeration;

@RestController
@RequestMapping("/api")
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);
    private final WebClient proxyWebClient;
    private final EventPublisherService eventPublisherService;
    private final MetricsService metricsService;

    public ProxyController(WebClient proxyWebClient,
                           EventPublisherService eventPublisherService,
                           MetricsService metricsService) {
        this.proxyWebClient = proxyWebClient;
        this.eventPublisherService = eventPublisherService;
        this.metricsService = metricsService;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request) throws IOException {
        long start = System.currentTimeMillis();
        TenantInfo tenant = TenantContext.get();
        String tenantId = tenant != null ? tenant.tenantId() : "unknown";

        String backendPath = request.getRequestURI().replaceFirst("^/api", "");
        if (backendPath.isEmpty()) backendPath = "/";
        String qs = request.getQueryString();
        String targetUrl = qs != null ? backendPath + "?" + qs : backendPath;
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String path = request.getRequestURI();

        try {
            WebClient.RequestBodySpec spec = proxyWebClient.method(method).uri(targetUrl);

            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String h = headers.nextElement();
                if (!h.equalsIgnoreCase("host") && !h.equalsIgnoreCase("content-length")
                && !h.equalsIgnoreCase("x-api-key")
                && !h.equalsIgnoreCase("transfer-encoding")) {
                    spec.header(h, request.getHeader(h));
                }
            }
            spec.header("X-Tenant-Id", tenantId);

            ResponseEntity<String> resp;
            if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
                byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
                resp = spec.body(BodyInserters.fromValue(body.length > 0 ? new String(body) : "{}"))
                        .retrieve().toEntity(String.class).block();
            } else {
                resp = spec.retrieve().toEntity(String.class).block();
            }

            long ms = System.currentTimeMillis() - start;
            int status = resp != null ? resp.getStatusCode().value() : 502;
            log.info("{} {} → {} ({}ms) tenant:{}", method, backendPath, status, ms, tenantId);

            metricsService.recordRequest(tenantId, status, ms);
            eventPublisherService.publishRequestLog(
                new RequestLogEvent(tenantId, null, method.name(), path, status, ms)
            );

            if (resp == null) return ResponseEntity.status(502).body("{\"error\":\"bad_gateway\"}");
            return ResponseEntity.status(resp.getStatusCode())
                .headers(h -> resp.getHeaders().forEach((key, values) -> {
                    if (!key.equalsIgnoreCase("Transfer-Encoding")
                            && !key.equalsIgnoreCase("Content-Length")) {
                        h.addAll(key, values);
                    }
                }))
                .body(resp.getBody());

        } catch (WebClientResponseException e) {
            long ms = System.currentTimeMillis() - start;
            metricsService.recordRequest(tenantId, e.getStatusCode().value(), ms);
            eventPublisherService.publishRequestLog(
                new RequestLogEvent(tenantId, null, method.name(), path, e.getStatusCode().value(), ms)
            );
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - start;
            log.error("Proxy error: {}", e.getMessage());
            metricsService.recordRequest(tenantId, 502, ms);
            eventPublisherService.publishRequestLog(
                new RequestLogEvent(tenantId, null, method.name(), path, 502, ms)
            );
            return ResponseEntity.status(502).body("{\"error\":\"bad_gateway\"}");
        }
    }
}