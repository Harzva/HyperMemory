package com.example.rag.controller;

import com.example.rag.dto.ChatRequest;
import com.example.rag.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST controller providing an endpoint for chat interactions. Clients
 * post a user question and receive a stream of partial answers via SSE.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * Chat endpoint using Server‑Sent Events. The response is streamed token by
     * token, emulating a "typing" effect. For brevity we simply wrap the
     * full answer into a single SSE event; you can break the answer into
     * smaller chunks and delay emission to simulate streaming.
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@RequestBody ChatRequest request) {
        String result = ragService.ask(request.getConversationId(), request.getUserInput());
        // Return a flux with a single item. Clients should subscribe to the event stream.
        return ResponseEntity.ok(Flux.just(result));
    }
}