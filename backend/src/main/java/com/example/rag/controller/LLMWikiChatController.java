package com.example.rag.controller;

import com.example.rag.dto.AnswerWithSources;
import com.example.rag.dto.ChatRequest;
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

    public LLMWikiChatController(LLMWikiService llmWikiService) {
        this.llmWikiService = llmWikiService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@Valid @RequestBody ChatRequest request) {
        String result = llmWikiService.query(request.getUserInput(), request.getTenantId());
        return ResponseEntity.ok(Flux.just(result));
    }

    @PostMapping("/with-sources")
    public ResponseEntity<AnswerWithSources> chatWithSources(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(llmWikiService.queryWithSources(request.getUserInput(), request.getTenantId()));
    }
}
