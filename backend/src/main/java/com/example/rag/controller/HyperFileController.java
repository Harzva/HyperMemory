package com.example.rag.controller;

import com.example.rag.model.DocumentEntity;
import com.example.rag.service.DocumentService;
import com.example.rag.service.HyperMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller for uploading documents to the HyperMemory system.  This
 * endpoint persists the file using the existing DocumentService (which
 * stores the file in MinIO and creates an embedding for RAG), extracts
 * plain text content, and ingests that content into the HyperMemory
 * service.  Separating this from other upload endpoints ensures
 * existing functionality remains unaffected while enabling hyper memory
 * ingestion.
 */
@RestController
@RequestMapping("/api/hyper")
public class HyperFileController {

    private final DocumentService documentService;
    private final HyperMemoryService hyperMemoryService;

    @Autowired
    public HyperFileController(DocumentService documentService,
                                HyperMemoryService hyperMemoryService) {
        this.documentService = documentService;
        this.hyperMemoryService = hyperMemoryService;
    }

    /**
     * Upload a document and ingest it into the hyper memory.  The file
     * content is stored in MinIO and vectorised by DocumentService as
     * usual, then plain text is read from the file and passed to
     * HyperMemoryService.ingest so that it can populate both the wiki
     * and internal storage layers.
     *
     * @param file the uploaded file
     * @return the persisted DocumentEntity
     * @throws IOException if the file cannot be read
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentEntity> uploadToHyper(MultipartFile file) throws IOException {
        DocumentEntity entity = documentService.uploadDocument(file);
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            content = "";
        }
        hyperMemoryService.ingest(entity, content);
        return ResponseEntity.ok(entity);
    }
}