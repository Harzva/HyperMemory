package com.example.rag.controller;

import com.example.rag.model.DocumentEntity;
import com.example.rag.service.AccessControlService;
import com.example.rag.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller exposing endpoints for knowledge base management. Users can
 * upload documents which will be persisted and indexed for semantic search.
 */
@RestController
@RequestMapping("/api/documents")
public class FileController {

    private final DocumentService documentService;
    private final AccessControlService accessControlService;

    public FileController(DocumentService documentService,
                          AccessControlService accessControlService) {
        this.documentService = documentService;
        this.accessControlService = accessControlService;
    }

    /**
     * Upload a document to the knowledge base. The file must be submitted as
     * form-data under the "file" field.
     */
    @PostMapping
    public ResponseEntity<DocumentEntity> upload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "tenantId", required = false) String tenantId) throws IOException {
        DocumentEntity saved = documentService.uploadDocument(file, accessControlService.resolveTenantId(tenantId));
        return ResponseEntity.ok(saved);
    }
}
