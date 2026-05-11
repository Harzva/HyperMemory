package com.example.rag.controller;

import com.example.rag.model.DocumentEntity;
import com.example.rag.service.DocumentService;
import com.example.rag.service.HierarchyMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller for uploading documents to the HierarchyMemory system.  This
 * endpoint persists the file using the existing DocumentService (which
 * stores the file in MinIO and creates an embedding for RAG), extracts
 * plain text content, and ingests that content into the HierarchyMemory
 * via its service.  Separating this from the standard RAG upload
 * endpoint ensures the original RAG pipeline continues to work
 * unmodified while enabling hierarchical memory ingestion.
 */
@RestController
@RequestMapping("/api/hierarchy")
public class HierarchyFileController {

    private final DocumentService documentService;
    private final HierarchyMemoryService hierarchyMemoryService;

    @Autowired
    public HierarchyFileController(DocumentService documentService,
                                   HierarchyMemoryService hierarchyMemoryService) {
        this.documentService = documentService;
        this.hierarchyMemoryService = hierarchyMemoryService;
    }

    /**
     * Upload a document and ingest it into the hierarchy memory.  The file
     * content is stored in MinIO and vectorised by DocumentService as
     * usual, then plain text is read from the file and passed to
     * HierarchyMemoryService.ingest so that it can populate both the wiki
     * and internal conversation memory layers.
     *
     * @param file the uploaded file
     * @return the persisted DocumentEntity
     * @throws IOException if the file cannot be read
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentEntity> uploadToHierarchy(MultipartFile file) throws IOException {
        // Store the file and create an embedding in the vector store.
        DocumentEntity entity = documentService.uploadDocument(file);
        // Extract the plain text content.  In a real implementation you
        // would handle PDFs, Word docs, etc.  For now we assume UTF‑8 text.
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            content = "";
        }
        // Ingest into the hierarchy memory service, which will update the wiki
        // and document store layers.
        hierarchyMemoryService.ingest(entity, content);
        return ResponseEntity.ok(entity);
    }
}