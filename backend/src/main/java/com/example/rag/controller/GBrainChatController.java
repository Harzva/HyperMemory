package com.example.rag.controller;

import com.example.rag.dto.ChatRequest;
import com.example.rag.service.GBrainService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Controller exposing a chat endpoint for the GBrain agent. This endpoint
 * returns answers based on the persistent wiki memory managed by
 * {@link GBrainService}. In a complete system, this could also invoke
 * skills or schedule tasks based on the question.
 */
@RestController
@RequestMapping("/api/gbrain/chat")
public class GBrainChatController {

    private final GBrainService gBrainService;

    public GBrainChatController(GBrainService gBrainService) {
        this.gBrainService = gBrainService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@Valid @RequestBody ChatRequest request) {
        String result = gBrainService.ask(request.getUserInput(), request.getTenantId());
        return ResponseEntity.ok(Flux.just(result));
    }
}
