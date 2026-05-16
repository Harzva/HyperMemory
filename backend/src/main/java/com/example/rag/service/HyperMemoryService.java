package com.example.rag.service;

import com.example.rag.dto.AnswerWithSources;
import com.example.rag.model.DocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HyperMemoryService provides a compact, bounded memory layer on top of wiki
 * retrieval. Document content stays in the wiki/retrieval layers, while this
 * service keeps recent conversation turns for final response aggregation.
 */
@Service
public class HyperMemoryService {

    private final LLMWikiService wikiService;
    private final Map<String, Deque<String>> conversationMemoryByTenant = new ConcurrentHashMap<>();
    private final int maxConversationMessages;

    @Autowired
    public HyperMemoryService(LLMWikiService wikiService,
                              @Value("${hypermemory.max-conversation-messages:20}") int maxConversationMessages) {
        this.wikiService = wikiService;
        this.maxConversationMessages = Math.max(1, maxConversationMessages);
    }

    public void ingest(DocumentEntity document, String content) {
        if (document != null) {
            wikiService.ingest(document, content);
        }
    }

    public void rememberMessage(String message) {
        rememberMessage(message, null);
    }

    public void rememberMessage(String message, String tenantId) {
        if (!StringUtils.hasText(message)) {
            return;
        }

        Deque<String> conversationMemory = memoryForTenant(tenantId);
        conversationMemory.addLast(message.strip());
        trimConversationMemory(conversationMemory);
    }

    public String query(String question) {
        return query(question, null);
    }

    public String query(String question, String tenantId) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        String wiki = wikiService.query(question, normalizedTenantId);
        StringBuilder answer = new StringBuilder(wiki);
        List<String> recentMessages = new ArrayList<>(memoryForTenant(normalizedTenantId));
        if (!recentMessages.isEmpty()) {
            answer.append("\n\n[Conversation Memory]\n");
            for (String message : recentMessages) {
                answer.append(message).append("\n");
            }
        }
        return answer.toString();
    }

    public AnswerWithSources queryWithSources(String question) {
        return queryWithSources(question, null);
    }

    public AnswerWithSources queryWithSources(String question, String tenantId) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        AnswerWithSources wiki = wikiService.queryWithSources(question, normalizedTenantId);
        StringBuilder answer = new StringBuilder(wiki.getAnswer());
        List<String> recentMessages = new ArrayList<>(memoryForTenant(normalizedTenantId));
        if (!recentMessages.isEmpty()) {
            answer.append("\n\n[Conversation Memory]\n");
            for (String message : recentMessages) {
                answer.append(message).append("\n");
            }
        }
        return AnswerWithSources.of(answer.toString(), wiki.getSources());
    }

    private Deque<String> memoryForTenant(String tenantId) {
        return conversationMemoryByTenant.computeIfAbsent(
                normalizeTenantId(tenantId),
                key -> new ConcurrentLinkedDeque<>());
    }

    private void trimConversationMemory(Deque<String> conversationMemory) {
        while (conversationMemory.size() > maxConversationMessages) {
            conversationMemory.pollFirst();
        }
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? "default"
                : tenantId.trim().toLowerCase(Locale.ROOT);
    }
}
