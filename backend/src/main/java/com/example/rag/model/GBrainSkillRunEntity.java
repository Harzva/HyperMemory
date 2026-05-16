package com.example.rag.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
        name = "gbrain_skill_runs",
        indexes = {
                @Index(name = "idx_gbrain_runs_tenant", columnList = "tenant_id"),
                @Index(name = "idx_gbrain_runs_created_at", columnList = "created_at")
        }
)
public class GBrainSkillRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId = "default";

    @Column(name = "skill_name", nullable = false, length = 128)
    private String skillName;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void setTenantId(String tenantId) {
        this.tenantId = normalizeTenantId(tenantId);
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDetails(String details) {
        this.details = details;
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
