package com.example.rag.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
        name = "hyper_memory_records",
        indexes = {
                @Index(name = "idx_hyper_memory_tenant", columnList = "tenant_id"),
                @Index(name = "idx_hyper_memory_created_at", columnList = "created_at")
        }
)
public class HyperMemoryRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId = "default";

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = normalizeTenantId(tenantId);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    void normalize() {
        tenantId = normalizeTenantId(tenantId);
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private String normalizeTenantId(String value) {
        return value == null || value.isBlank()
                ? "default"
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
