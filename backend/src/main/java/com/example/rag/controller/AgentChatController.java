package com.example.rag.controller;

import com.example.rag.dto.ChatRequest;
import com.example.rag.service.AccessControlService;
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
    private final AccessControlService accessControlService;

    public AgentChatController(AgentService agentService,
                               AccessControlService accessControlService) {
        this.agentService = agentService;
        this.accessControlService = accessControlService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@Valid @RequestBody ChatRequest request) {
        String tenantId = accessControlService.resolveTenantId(request.getTenantId());
        String result = agentService.ask(request.getUserInput(), tenantId);
        return ResponseEntity.ok(Flux.just(result));
    }
}
