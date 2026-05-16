package com.example.rag.config;

import java.util.Set;

public record ApiPrincipal(String name, String tenantId, Set<String> roles) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
