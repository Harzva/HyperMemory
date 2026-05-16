package com.example.rag.service;

import com.example.rag.dto.AnswerWithSources;
import com.example.rag.model.DocumentEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Wiki-style memory facade over the shared retrieval core. Uploaded documents
 * are still indexed by DocumentService; wiki mode formats retrieved chunks as
 * readable source pages instead of returning every stored page blindly.
 */
@Service
public class LLMWikiService {

    private static final int TOP_K = 5;

    private final RetrievalContextService retrievalContextService;
    private final Map<String, Map<Long, String>> wikiPagesByTenant = new ConcurrentHashMap<>();

    public LLMWikiService(RetrievalContextService retrievalContextService) {
        this.retrievalContextService = retrievalContextService;
    }

    public void ingest(DocumentEntity document, String content) {
        if (document == null) {
            return;
        }

        StringBuilder page = new StringBuilder();
        page.append("# ").append(document.getFilename()).append("\n\n");
        if (content != null && !content.isBlank()) {
            int limit = Math.min(content.length(), 500);
            page.append(content, 0, limit);
            if (content.length() > limit) {
                page.append("...\n");
            }
        } else {
            page.append("(no extractable text available)\n");
        }
        wikiPagesByTenant
                .computeIfAbsent(normalizeTenantId(document.getTenantId()), key -> new ConcurrentHashMap<>())
                .put(document.getId(), page.toString());
    }

    public String query(String question) {
        return queryWithSources(question).getAnswer();
    }

    public String query(String question, String tenantId) {
        return queryWithSources(question, tenantId).getAnswer();
    }

    public AnswerWithSources queryWithSources(String question) {
        return queryWithSources(question, null);
    }

    public AnswerWithSources queryWithSources(String question, String tenantId) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        RetrievalContextService.RetrievalResult result = retrievalContextService.retrieve(question, TOP_K, normalizedTenantId);
        if (!result.getFormattedContext().isBlank()) {
            return AnswerWithSources.of("## Retrieved Wiki Context\n\n" + result.getFormattedContext(), result.getCitations());
        }

        Map<Long, String> wikiPages = wikiPagesByTenant.getOrDefault(normalizedTenantId, Collections.emptyMap());
        if (wikiPages.isEmpty()) {
            return AnswerWithSources.of("No wiki pages or retrieved context are available yet.", Collections.emptyList());
        }

        return AnswerWithSources.of(
                wikiPages.values().stream().collect(Collectors.joining("\n\n---\n\n")),
                Collections.emptyList());
    }

    public int pageCount() {
        return wikiPagesByTenant.values().stream().mapToInt(Map::size).sum();
    }

    public int totalPageCharacters() {
        return wikiPagesByTenant.values().stream()
                .flatMap(pages -> pages.values().stream())
                .mapToInt(String::length)
                .sum();
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? "default"
                : tenantId.trim().toLowerCase(Locale.ROOT);
    }
}
