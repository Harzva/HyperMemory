package com.example.rag.service;

import com.example.rag.config.BotGatewayProperties;
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
    private final HierarchyMemoryService hierarchyMemoryService;
    private final HyperMemoryService hyperMemoryService;

    public BotGatewayService(BotGatewayProperties properties,
                             RagService ragService,
                             LLMWikiService wikiService,
                             AgentService agentService,
                             GBrainService gBrainService,
                             HierarchyMemoryService hierarchyMemoryService,
                             HyperMemoryService hyperMemoryService) {
        this.properties = properties;
        this.ragService = ragService;
        this.wikiService = wikiService;
        this.agentService = agentService;
        this.gBrainService = gBrainService;
        this.hierarchyMemoryService = hierarchyMemoryService;
        this.hyperMemoryService = hyperMemoryService;
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

        String answer = switch (mode) {
            case "hyper" -> hyperMemoryService.query(text);
            case "hierarchy" -> hierarchyMemoryService.query(text);
            case "agent" -> agentService.ask(text);
            case "gbrain" -> gBrainService.ask(text);
            case "wiki" -> wikiService.query(text);
            case "rag" -> ragService.ask(conversationId, text);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported bot mode: " + mode);
        };
        return BotMessageResponse.success(channel, conversationId, mode, answer);
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
