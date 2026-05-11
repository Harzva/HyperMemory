package com.example.rag.controller;

import com.example.rag.dto.ChatRequest;
import com.example.rag.service.AgentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST controller exposing an endpoint for agent‑based chat interactions. This
 * endpoint delegates the user question to an {@link AgentService} which
 * uses LangChain4j's Agent framework to decide whether to call
 * registered tools or answer directly. The response is streamed as a
 * Server‑Sent Event (SSE) so clients can display partial answers as
 * they arrive. In this simple implementation we emit the full
 * response as a single SSE event.
 */
@RestController
@RequestMapping("/api/agent/chat")
public class AgentChatController {

    private final AgentService agentService;

    public AgentChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Chat endpoint for the agent. Accepts a JSON body with a
     * conversation identifier (ignored here) and a user question. The
     * question is passed to the agent service, and the result is
     * returned as a flux with a single event. Clients should
     * subscribe to the event stream for incremental display of the
     * answer.
     *
     * @param request input containing conversationId and userInput
     * @return a {@code Flux<String>} containing the agent's answer
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chat(@RequestBody ChatRequest request) {
        String result = agentService.ask(request.getUserInput());
        return ResponseEntity.ok(Flux.just(result));
    }
}