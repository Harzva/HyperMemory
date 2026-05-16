package com.example.rag.controller;

import com.example.rag.dto.AnswerWithSources;
import com.example.rag.dto.ChatRequest;
import com.example.rag.service.AccessControlService;
import com.example.rag.service.LLMWikiService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chat endpoint for wiki-style memory over the shared retrieval core.
 */
@RestController
@RequestMapping("/api/wiki/chat")
public class LLMWikiChatController {

    private final LLMWikiService llmWikiService;
    private final AccessControlService accessControlService;

    public LLMWikiChatController(LLMWikiService llmWikiService,
                                 AccessControlService accessControlService) {
        this.llmWikiService = llmWikiService;
        this.accessControlService = accessControlService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@Valid @RequestBody ChatRequest request) {
        String tenantId = accessControlService.resolveTenantId(request.getTenantId());
        String result = llmWikiService.query(request.getUserInput(), tenantId);
        return ResponseEntity.ok(Flux.just(result));
    }

    @PostMapping("/with-sources")
    public ResponseEntity<AnswerWithSources> chatWithSources(@Valid @RequestBody ChatRequest request) {
        String tenantId = accessControlService.resolveTenantId(request.getTenantId());
        return ResponseEntity.ok(llmWikiService.queryWithSources(request.getUserInput(), tenantId));
    }
}
