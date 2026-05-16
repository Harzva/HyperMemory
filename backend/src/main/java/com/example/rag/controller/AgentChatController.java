package com.example.rag.controller;

import com.example.rag.dto.ChatRequest;
import com.example.rag.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chat endpoint for tool-using agent interactions.
 */
@RestController
@RequestMapping("/api/agent/chat")
public class AgentChatController {

    private final AgentService agentService;

    public AgentChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@Valid @RequestBody ChatRequest request) {
        String result = agentService.ask(request.getUserInput(), request.getTenantId());
        return ResponseEntity.ok(Flux.just(result));
    }
}
