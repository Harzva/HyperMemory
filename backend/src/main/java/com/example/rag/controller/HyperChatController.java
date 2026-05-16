package com.example.rag.controller;

import com.example.rag.dto.AnswerWithSources;
import com.example.rag.dto.ChatRequest;
import com.example.rag.service.HyperMemoryService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chat controller for the HyperMemory system.  This endpoint records
 * user and assistant messages into a conversation memory and returns
 * an answer composed from both the wiki and conversation context.  It
 * streams the answer as an SSE so that the front end can display
 * responses progressively.
 */
@RestController
@RequestMapping("/api/hyper/chat")
public class HyperChatController {

    private final HyperMemoryService hyperMemoryService;

    public HyperChatController(HyperMemoryService hyperMemoryService) {
        this.hyperMemoryService = hyperMemoryService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@Valid @RequestBody ChatRequest request) {
        String userMessage = "User: " + request.getUserInput();
        hyperMemoryService.rememberMessage(userMessage, request.getTenantId());
        String answer = hyperMemoryService.query(request.getUserInput(), request.getTenantId());
        hyperMemoryService.rememberMessage("Assistant: " + answer, request.getTenantId());
        return ResponseEntity.ok(Flux.just(answer));
    }

    @PostMapping("/with-sources")
    public ResponseEntity<AnswerWithSources> chatWithSources(@Valid @RequestBody ChatRequest request) {
        String userMessage = "User: " + request.getUserInput();
        hyperMemoryService.rememberMessage(userMessage, request.getTenantId());
        AnswerWithSources result = hyperMemoryService.queryWithSources(request.getUserInput(), request.getTenantId());
        hyperMemoryService.rememberMessage("Assistant: " + result.getAnswer(), request.getTenantId());
        return ResponseEntity.ok(result);
    }
}
