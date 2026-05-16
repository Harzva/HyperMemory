package com.example.rag.repository;

import com.example.rag.model.HyperMemoryRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HyperMemoryRecordRepository extends JpaRepository<HyperMemoryRecordEntity, Long> {
    List<HyperMemoryRecordEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
