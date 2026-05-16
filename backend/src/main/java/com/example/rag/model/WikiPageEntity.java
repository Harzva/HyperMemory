package com.example.rag.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
        name = "wiki_pages",
        uniqueConstraints = @UniqueConstraint(name = "uk_wiki_pages_tenant_document", columnNames = {"tenant_id", "document_id"}),
        indexes = {
                @Index(name = "idx_wiki_pages_tenant", columnList = "tenant_id"),
                @Index(name = "idx_wiki_pages_updated_at", columnList = "updated_at")
        }
)
public class WikiPageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId = "default";

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = normalizeTenantId(tenantId);
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        tenantId = normalizeTenantId(tenantId);
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    private String normalizeTenantId(String value) {
        return value == null || value.isBlank()
                ? "default"
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
