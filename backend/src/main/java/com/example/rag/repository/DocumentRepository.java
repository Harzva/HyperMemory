package com.example.rag.repository;

import com.example.rag.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for storing and querying DocumentEntity records.
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    // Additional query methods can be defined here when needed
}