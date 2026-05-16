package com.example.rag.repository;

import com.example.rag.model.WikiPageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WikiPageRepository extends JpaRepository<WikiPageEntity, Long> {
    Optional<WikiPageEntity> findByTenantIdAndDocumentId(String tenantId, Long documentId);

    List<WikiPageEntity> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
