package com.example.rag.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a document stored in the knowledge base.
 * Each document record tracks the original file name, storage key in MinIO,
 * and metadata about ingestion. The embeddings for the document live in Milvus.
 */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The original filename uploaded by the user. */
    @Column(nullable = false)
    private String filename;

    /** The MIME type of the uploaded file. */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    /**
     * The key (object name) under which the file is stored in MinIO. This could
     * include a bucket prefix and randomised path for collision avoidance.
     */
    @Column(nullable = false, unique = true)
    private String objectKey;

    /**
     * A timestamp indicating when the document was uploaded and ingested. Useful
     * for ordering and TTL cleanup.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}