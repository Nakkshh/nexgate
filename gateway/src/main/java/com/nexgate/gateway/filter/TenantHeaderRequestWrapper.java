package com.nexgate.gateway.filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class TenantHeaderRequestWrapper extends HttpServletRequestWrapper {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private final String tenantId;

    public TenantHeaderRequestWrapper(HttpServletRequest request, String tenantId) {
        super(request);
        this.tenantId = tenantId;
    }

    @Override
    public String getHeader(String name) {
        if (TENANT_HEADER.equalsIgnoreCase(name)) return tenantId;
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (TENANT_HEADER.equalsIgnoreCase(name))
            return Collections.enumeration(List.of(tenantId));
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>();
        Enumeration<String> orig = super.getHeaderNames();
        while (orig.hasMoreElements()) names.add(orig.nextElement());
        names.add(TENANT_HEADER);
        return Collections.enumeration(names);
    }
}