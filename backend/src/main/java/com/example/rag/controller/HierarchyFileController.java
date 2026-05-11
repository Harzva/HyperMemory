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
 * Controller for uploading documents to the HierarchyMemory system.
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

    @PostMapping("/upload")
    public ResponseEntity<DocumentEntity> uploadToHierarchy(MultipartFile file) throws IOException {
        DocumentEntity entity = documentService.uploadDocument(file);

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            content = "";
        }

        hierarchyMemoryService.ingest(entity, content);
        return ResponseEntity.ok(entity);
    }
}
