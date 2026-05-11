package com.example.rag.controller;

import com.example.rag.dto.ChatRequest;
import com.example.rag.service.HierarchyMemoryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chat controller for the HierarchyMemory system. This endpoint records
 * user and assistant messages into a simple conversation memory and
 * returns an answer derived from the wiki and conversation history.
 */
@RestController
@RequestMapping("/api/hierarchy/chat")
public class HierarchyChatController {

    private final HierarchyMemoryService hierarchyMemoryService;

    public HierarchyChatController(HierarchyMemoryService hierarchyMemoryService) {
        this.hierarchyMemoryService = hierarchyMemoryService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@RequestBody ChatRequest request) {
        String userMessage = "User: " + request.getUserInput();
        // store user message
        hierarchyMemoryService.rememberMessage(userMessage);
        // derive answer from hierarchy memory
        String answer = hierarchyMemoryService.query(request.getUserInput());
        // store assistant message
        hierarchyMemoryService.rememberMessage("Assistant: " + answer);
        return ResponseEntity.ok(Flux.just(answer));
    }
}