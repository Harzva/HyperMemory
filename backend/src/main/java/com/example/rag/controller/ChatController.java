package com.example.rag.controller;

import com.example.rag.dto.AnswerWithSources;
import com.example.rag.dto.ChatRequest;
import com.example.rag.service.AccessControlService;
import com.example.rag.service.RagService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * REST controller for direct RAG chat interactions.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;
    private final AccessControlService accessControlService;

    public ChatController(RagService ragService,
                          AccessControlService accessControlService) {
        this.ragService = ragService;
        this.accessControlService = accessControlService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@Valid @RequestBody ChatRequest request) {
        String tenantId = accessControlService.resolveTenantId(request.getTenantId());
        String result = ragService.ask(request.getConversationId(), request.getUserInput(), tenantId);
        return ResponseEntity.ok(Flux.just(result));
    }

    @PostMapping("/with-sources")
    public ResponseEntity<AnswerWithSources> chatWithSources(@Valid @RequestBody ChatRequest request) {
        String tenantId = accessControlService.resolveTenantId(request.getTenantId());
        return ResponseEntity.ok(ragService.askWithSources(request.getConversationId(), request.getUserInput(), tenantId));
    }
}
