package com.example.rag.service;

import com.example.rag.config.BotGatewayProperties;
import com.example.rag.dto.AnswerWithSources;
import com.example.rag.dto.BotMessageRequest;
import com.example.rag.dto.BotMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class BotGatewayService {
    private final BotGatewayProperties properties;
    private final RagService ragService;
    private final LLMWikiService wikiService;
    private final AgentService agentService;
    private final GBrainService gBrainService;
    private final HyperMemoryService hyperMemoryService;
    private final BotIdempotencyService idempotencyService;

    public BotGatewayService(BotGatewayProperties properties,
                             RagService ragService,
                             LLMWikiService wikiService,
                             AgentService agentService,
                             GBrainService gBrainService,
                             HyperMemoryService hyperMemoryService,
                             BotIdempotencyService idempotencyService) {
        this.properties = properties;
        this.ragService = ragService;
        this.wikiService = wikiService;
        this.agentService = agentService;
        this.gBrainService = gBrainService;
        this.hyperMemoryService = hyperMemoryService;
        this.idempotencyService = idempotencyService;
    }

    public BotMessageResponse handle(BotMessageRequest request) {
        String channel = normalize(request.getChannel(), "unknown");
        String mode = normalize(request.getMode(), properties.getDefaultMode());
        String text = request.getText();
        if (!StringUtils.hasText(text)) {
            return BotMessageResponse.failure(channel, request.getConversationId(), mode, "text is required");
        }

        assertModeAllowed(channel, mode);
        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : channel + ":" + normalize(request.getSenderId(), "anonymous");

        String messageId = request.getMessageId();
        String tenantId = normalize(request.getTenantId(), "default");
        boolean idempotencyAcquired = false;
        if (StringUtils.hasText(messageId)) {
            idempotencyAcquired = idempotencyService.acquire(tenantId, channel, messageId);
            if (!idempotencyAcquired) {
                return BotMessageResponse.success(channel, conversationId, mode, "Duplicate message ignored.");
            }
        }

        try {
            AnswerWithSources result;
            switch (mode) {
                case "hyper" -> result = hyperMemoryService.queryWithSources(text);
                case "wiki" -> result = wikiService.queryWithSources(text);
                case "rag" -> result = ragService.askWithSources(conversationId, text);
                case "agent" -> {
                    return BotMessageResponse.success(channel, conversationId, mode, agentService.ask(text));
                }
                case "gbrain" -> {
                    return BotMessageResponse.success(channel, conversationId, mode, gBrainService.ask(text));
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported bot mode: " + mode);
            }
            return BotMessageResponse.successWithSources(channel, conversationId, mode, result.getAnswer(), result.getSources());
        } catch (RuntimeException e) {
            if (idempotencyAcquired) {
                idempotencyService.release(tenantId, channel, messageId);
            }
            throw e;
        }
    }

    private void assertModeAllowed(String channel, String mode) {
        var allowedModes = properties.channel(channel).getAllowedModes();
        if (!allowedModes.isEmpty() && allowedModes.stream().noneMatch(allowed -> mode.equalsIgnoreCase(allowed))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mode is not allowed for channel: " + mode);
        }
    }

    private String normalize(String value, String fallback) {
        String candidate = StringUtils.hasText(value) ? value : fallback;
        return candidate == null ? "" : candidate.trim().toLowerCase(Locale.ROOT);
    }
}
