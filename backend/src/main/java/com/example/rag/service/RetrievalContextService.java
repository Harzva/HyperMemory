package com.example.rag.service;

import com.example.rag.dto.SourceCitation;
import com.example.rag.model.DocumentChunkEntity;
import com.example.rag.model.DocumentEntity;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shared retrieval service used by both the direct RAG endpoint and agent
 * tools. Milvus returns vector item IDs, then this service hydrates the real
 * chunk text from MySQL so prompts contain grounded context instead of IDs.
 */
@Service
public class RetrievalContextService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalContextService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<?> embeddingStore;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public RetrievalContextService(EmbeddingModel embeddingModel,
                                   EmbeddingStore<?> embeddingStore,
                                   DocumentChunkRepository documentChunkRepository,
                                   DocumentRepository documentRepository) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
    }

    public static class RetrievalResult {
        private final String formattedContext;
        private final List<SourceCitation> citations;

        public RetrievalResult(String formattedContext, List<SourceCitation> citations) {
            this.formattedContext = formattedContext;
            this.citations = citations;
        }

        public String getFormattedContext() {
            return formattedContext;
        }

        public List<SourceCitation> getCitations() {
            return citations;
        }
    }

    public RetrievalResult retrieve(String query, int topK) {
        return retrieve(query, topK, null);
    }

    public RetrievalResult retrieve(String query, int topK, String tenantId) {
        if (query == null || query.isBlank() || topK <= 0) {
            return new RetrievalResult("", new ArrayList<>());
        }

        String normalizedTenantId = normalizeTenantId(tenantId);
        StringBuilder contextBuilder = new StringBuilder();
        List<SourceCitation> citations = new ArrayList<>();
        try {
            var queryEmbedding = embeddingModel.embed(query).content();
            var searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .build();
            var matches = embeddingStore.search(searchRequest).matches();
            int sourceNumber = 1;
            for (var match : matches) {
                int accepted = collectRetrievedResult(contextBuilder, citations, sourceNumber, match.embeddingId(), normalizedTenantId);
                sourceNumber += accepted;
            }
        } catch (Exception e) {
            log.warn("Context retrieval failed: {}", e.getMessage());
            return new RetrievalResult("", new ArrayList<>());
        }
        return new RetrievalResult(contextBuilder.toString(), citations);
    }

    private int collectRetrievedResult(StringBuilder contextBuilder, List<SourceCitation> citations,
                                       int sourceNumber, String itemId, String normalizedTenantId) {
        Long id = parseId(itemId);
        if (id == null) {
            return 0;
        }

        Optional<DocumentChunkEntity> chunk = documentChunkRepository.findById(id);
        if (chunk.isPresent()) {
            DocumentChunkEntity entity = chunk.get();
            DocumentEntity document = entity.getDocument();
            if (!normalizedTenantId.equals(normalizeTenantId(document.getTenantId()))) {
                return 0;
            }
            contextBuilder
                    .append("[Source ").append(sourceNumber).append("] ")
                    .append(document.getFilename())
                    .append(" #chunk ").append(entity.getChunkIndex())
                    .append("\n")
                    .append(entity.getContent())
                    .append("\n\n");
            String content = entity.getContent() == null ? "" : entity.getContent();
            String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            citations.add(new SourceCitation(sourceNumber, document.getId(),
                    document.getFilename(), entity.getChunkIndex(), preview));
            return 1;
        }

        Optional<DocumentEntity> docOpt = documentRepository.findById(id);
        if (docOpt.isPresent()) {
            DocumentEntity document = docOpt.get();
            if (!normalizedTenantId.equals(normalizeTenantId(document.getTenantId()))) {
                return 0;
            }
            contextBuilder
                    .append("[Source ").append(sourceNumber).append("] ")
                    .append(document.getFilename())
                    .append("\n")
                    .append("Legacy vector entry points to document metadata only. Re-upload this file to create searchable text chunks.")
                    .append("\n\n");
            citations.add(new SourceCitation(sourceNumber, document.getId(),
                    document.getFilename(), -1, "(legacy entry — re-upload for chunk retrieval)"));
            return 1;
        }
        return 0;
    }

    public String retrieveContext(String query, int topK) {
        return retrieve(query, topK).getFormattedContext();
    }

    public String retrieveContext(String query, int topK, String tenantId) {
        return retrieve(query, topK, tenantId).getFormattedContext();
    }

    private Long parseId(String itemId) {
        try {
            return Long.parseLong(itemId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? "default"
                : tenantId.trim().toLowerCase(Locale.ROOT);
    }
}
