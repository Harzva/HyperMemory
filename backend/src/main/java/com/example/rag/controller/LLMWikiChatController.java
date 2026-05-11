package com.example.rag.controller;

import com.example.rag.dto.ChatRequest;
import com.example.rag.service.LLMWikiService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Controller for chat interactions using the LLM‑Wiki system. Unlike the RAG
 * and agent endpoints, this controller does not perform retrieval from
 * embeddings. Instead it fetches concatenated wiki pages from the
 * {@link com.example.rag.service.LLMWikiService} and returns them as a
 * single Server‑Sent Event. This allows clients to reuse the same
 * streaming mechanism used by RAG and agent chats.
 */
@RestController
@RequestMapping("/api/wiki/chat")
public class LLMWikiChatController {

    private final LLMWikiService llmWikiService;

    public LLMWikiChatController(LLMWikiService llmWikiService) {
        this.llmWikiService = llmWikiService;
    }

    /**
     * Chat endpoint for the LLM‑Wiki system. It ignores the question and
     * returns the concatenated wiki pages as a single SSE event.
     *
     * @param request the chat request containing the user input (ignored)
     * @return a flux containing the wiki content as a single event
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@RequestBody ChatRequest request) {
        String result = llmWikiService.query(request.getUserInput());
        return ResponseEntity.ok(Flux.just(result));
    }
}