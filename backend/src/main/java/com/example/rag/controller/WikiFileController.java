package com.example.rag.controller;

import com.example.rag.model.DocumentEntity;
import com.example.rag.service.DocumentService;
import com.example.rag.service.LLMWikiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller for uploading documents specifically for the LLM Wiki system.
 */
@RestController
@RequestMapping("/api/wiki")
public class WikiFileController {

    private final DocumentService documentService;
    private final LLMWikiService llmWikiService;

    @Autowired
    public WikiFileController(DocumentService documentService,
                              LLMWikiService llmWikiService) {
        this.documentService = documentService;
        this.llmWikiService = llmWikiService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentEntity> uploadToWiki(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "tenantId", required = false) String tenantId) throws IOException {
        DocumentEntity entity = documentService.uploadDocument(file, tenantId);

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            content = "";
        }

        llmWikiService.ingest(entity, content);
        return ResponseEntity.ok(entity);
    }
}
