package com.example.rag.controller;

import com.example.rag.model.DocumentEntity;
import com.example.rag.service.DocumentService;
import com.example.rag.service.LLMWikiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller for uploading documents specifically for the LLM‑Wiki system. It
 * reuses the existing DocumentService to store the file and its embedding,
 * then ingests the plain text content into the LLM‑Wiki service. Note that
 * this endpoint is distinct from the RAG upload endpoint, preserving
 * separation between the two systems.
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

    /**
     * Upload a document and ingest it into the LLM‑Wiki. The file is stored
     * in MinIO and vectorised as usual, but its textual content is also
     * converted into a wiki page. This endpoint is separate from the RAG
     * upload endpoint to avoid affecting existing functionality.
     *
     * @param file the uploaded file
     * @return the persisted DocumentEntity
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentEntity> uploadToWiki(MultipartFile file) throws IOException {
        // Store file and generate embedding as usual
        DocumentEntity entity = documentService.uploadDocument(file);
        // Extract plain text from file; DocumentService currently reads only
        // UTF‑8 text. For other types of documents you would parse them here.
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            content = "";
        }
        // Ingest into wiki
        llmWikiService.ingest(entity, content);
        return ResponseEntity.ok(entity);
    }
}