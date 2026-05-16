package com.example.rag.service;

import com.example.rag.dto.AnswerWithSources;
import com.example.rag.dto.SourceCitation;
import com.example.rag.model.DocumentEntity;
import com.example.rag.model.HyperMemoryRecordEntity;
import com.example.rag.repository.HyperMemoryRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HyperMemoryService provides a compact, bounded memory layer on top of wiki
 * retrieval. Document content stays in the wiki/retrieval layers, while this
 * service keeps recent conversation turns for final response aggregation.
 */
@Service
public class HyperMemoryService {

    private final LLMWikiService wikiService;
    private final QaMetricsService qaMetricsService;
    private final HyperMemoryRecordRepository memoryRecordRepository;
    private final int maxConversationMessages;

    @Autowired
    public HyperMemoryService(LLMWikiService wikiService,
                              QaMetricsService qaMetricsService,
                              HyperMemoryRecordRepository memoryRecordRepository,
                              @Value("${hypermemory.max-conversation-messages:20}") int maxConversationMessages) {
        this.wikiService = wikiService;
        this.qaMetricsService = qaMetricsService;
        this.memoryRecordRepository = memoryRecordRepository;
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

        HyperMemoryRecordEntity record = new HyperMemoryRecordEntity();
        record.setTenantId(tenantId);
        record.setMessage(message.strip());
        memoryRecordRepository.save(record);
        trimConversationMemory(normalizeTenantId(tenantId));
    }

    public String query(String question) {
        return query(question, null);
    }

    public String query(String question, String tenantId) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        String wiki = wikiService.query(question, normalizedTenantId);
        StringBuilder answer = new StringBuilder(wiki);
        List<String> recentMessages = recentMessages(normalizedTenantId);
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
        AnswerWithSources result = qaMetricsService.recordOperation("queryWithSources", "hyper", tenantId, () -> {
            String normalizedTenantId = normalizeTenantId(tenantId);
            AnswerWithSources wiki = wikiService.queryWithSources(question, normalizedTenantId);
            StringBuilder answer = new StringBuilder(wiki.getAnswer());
            List<String> recentMessages = recentMessages(normalizedTenantId);
            if (!recentMessages.isEmpty()) {
                answer.append("\n\n[Conversation Memory]\n");
                for (String message : recentMessages) {
                    answer.append(message).append("\n");
                }
            }
            return AnswerWithSources.of(answer.toString(), wiki.getSources());
        });
        List<SourceCitation> sources = result.getSources();
        qaMetricsService.recordSourceCount("hyper", tenantId, sources == null ? 0 : sources.size());
        return result;
    }

    private List<String> recentMessages(String tenantId) {
        List<HyperMemoryRecordEntity> records = memoryRecordRepository.findByTenantIdOrderByCreatedAtDesc(normalizeTenantId(tenantId));
        List<String> messages = new ArrayList<>();
        int limit = Math.min(maxConversationMessages, records.size());
        for (int i = limit - 1; i >= 0; i--) {
            messages.add(records.get(i).getMessage());
        }
        return messages;
    }

    private void trimConversationMemory(String tenantId) {
        List<HyperMemoryRecordEntity> records = memoryRecordRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        if (records.size() <= maxConversationMessages) {
            return;
        }
        memoryRecordRepository.deleteAll(records.subList(maxConversationMessages, records.size()));
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? "default"
                : tenantId.trim().toLowerCase(Locale.ROOT);
    }
}
