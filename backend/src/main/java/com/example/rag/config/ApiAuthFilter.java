package com.example.rag.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiAuthFilter extends OncePerRequestFilter {

    private static final ThreadLocal<ApiPrincipal> CURRENT = new ThreadLocal<>();

    @Value("${api.auth.enabled:true}")
    private boolean enabled;

    @Value("${api.auth.tokens:admin|change-me-admin-token|default|ADMIN,USER}")
    private String tokenSpecs;

    public static ApiPrincipal currentPrincipal() {
        return CURRENT.get();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (!enabled || isPublicRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            ApiPrincipal principal = authenticate(extractToken(request));
            if (principal == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing or invalid API token\"}");
                return;
            }

            CURRENT.set(principal);
            filterChain.doFilter(request, response);
        } finally {
            CURRENT.remove();
        }
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || path.startsWith("/actuator/prometheus")
                || path.startsWith("/api/bot/");
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return bearer.substring(7).trim();
        }
        return request.getHeader("X-API-Token");
    }

    private ApiPrincipal authenticate(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        for (Map.Entry<String, ApiPrincipal> entry : configuredTokens().entrySet()) {
            if (MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), entry.getKey().getBytes(StandardCharsets.UTF_8))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, ApiPrincipal> configuredTokens() {
        Map<String, ApiPrincipal> tokens = new ConcurrentHashMap<>();
        for (String spec : tokenSpecs.split(";")) {
            String[] parts = spec.split("\\|");
            if (parts.length < 4 || !StringUtils.hasText(parts[1])) {
                continue;
            }
            String tenantId = normalizeTenantId(parts[2]);
            Set<String> roles = Arrays.stream(parts[3].split(","))
                    .map(role -> role.trim().toUpperCase(Locale.ROOT))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toUnmodifiableSet());
            tokens.put(parts[1].trim(), new ApiPrincipal(parts[0].trim(), tenantId, roles));
        }
        return tokens;
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? "default"
                : tenantId.trim().toLowerCase(Locale.ROOT);
    }
}
