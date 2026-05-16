package com.example.rag.service;

import com.example.rag.dto.AnswerWithSources;
import com.example.rag.dto.SourceCitation;
import com.example.rag.model.DocumentEntity;
import com.example.rag.model.WikiPageEntity;
import com.example.rag.repository.WikiPageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.Locale;
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
    private final QaMetricsService qaMetricsService;
    private final WikiPageRepository wikiPageRepository;

    public LLMWikiService(RetrievalContextService retrievalContextService,
                          QaMetricsService qaMetricsService,
                          WikiPageRepository wikiPageRepository) {
        this.retrievalContextService = retrievalContextService;
        this.qaMetricsService = qaMetricsService;
        this.wikiPageRepository = wikiPageRepository;
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
        String tenantId = normalizeTenantId(document.getTenantId());
        WikiPageEntity wikiPage = wikiPageRepository.findByTenantIdAndDocumentId(tenantId, document.getId())
                .orElseGet(WikiPageEntity::new);
        wikiPage.setTenantId(tenantId);
        wikiPage.setDocumentId(document.getId());
        wikiPage.setContent(page.toString());
        wikiPage.setUpdatedAt(LocalDateTime.now());
        wikiPageRepository.save(wikiPage);
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
        AnswerWithSources result = qaMetricsService.recordOperation("queryWithSources", "llm-wiki", tenantId, () -> {
            String normalizedTenantId = normalizeTenantId(tenantId);
            RetrievalContextService.RetrievalResult retrieval = retrievalContextService.retrieve(question, TOP_K, normalizedTenantId);
            if (!retrieval.getFormattedContext().isBlank()) {
                return AnswerWithSources.of("## Retrieved Wiki Context\n\n" + retrieval.getFormattedContext(), retrieval.getCitations());
            }

            List<WikiPageEntity> wikiPages = wikiPageRepository.findByTenantIdOrderByUpdatedAtDesc(normalizedTenantId);
            if (wikiPages.isEmpty()) {
                return AnswerWithSources.of("No wiki pages or retrieved context are available yet.", Collections.emptyList());
            }

            return AnswerWithSources.of(
                    wikiPages.stream().map(WikiPageEntity::getContent).collect(Collectors.joining("\n\n---\n\n")),
                    Collections.emptyList());
        });
        List<SourceCitation> sources = result.getSources();
        qaMetricsService.recordSourceCount("llm-wiki", tenantId, sources == null ? 0 : sources.size());
        return result;
    }

    public int pageCount() {
        return (int) wikiPageRepository.count();
    }

    public int totalPageCharacters() {
        return wikiPageRepository.findAll().stream()
                .map(WikiPageEntity::getContent)
                .mapToInt(String::length)
                .sum();
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? "default"
                : tenantId.trim().toLowerCase(Locale.ROOT);
    }
}
