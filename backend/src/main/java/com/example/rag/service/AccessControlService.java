package com.example.rag.service;

import com.example.rag.config.ApiAuthFilter;
import com.example.rag.config.ApiPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class AccessControlService {

    public String resolveTenantId(String requestedTenantId) {
        String normalized = normalizeTenantId(requestedTenantId);
        ApiPrincipal principal = ApiAuthFilter.currentPrincipal();
        if (principal == null || principal.hasRole("ADMIN")) {
            return normalized;
        }
        if (!principal.tenantId().equals(normalized)) {
            throw new ResponseStatusException(FORBIDDEN, "Token is not allowed to access tenant: " + normalized);
        }
        return principal.tenantId();
    }

    public void requireAdmin() {
        ApiPrincipal principal = ApiAuthFilter.currentPrincipal();
        if (principal != null && !principal.hasRole("ADMIN")) {
            throw new ResponseStatusException(FORBIDDEN, "Admin role is required");
        }
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? "default"
                : tenantId.trim().toLowerCase(Locale.ROOT);
    }
}
