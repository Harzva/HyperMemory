package com.example.rag.service;

import com.example.rag.model.DocumentChunkEntity;
import com.example.rag.model.DocumentEntity;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shared retrieval service used by both the direct RAG endpoint and agent
 * tools. Milvus returns vector item IDs, then this service hydrates the real
 * chunk text from MySQL so prompts contain grounded context instead of IDs.
 */
@Service
public class RetrievalContextService {

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

    public String retrieveContext(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder();
        try {
            var queryEmbedding = embeddingModel.embed(query).content();
            var searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .build();
            var matches = embeddingStore.search(searchRequest).matches();
            int sourceNumber = 1;
            for (var match : matches) {
                appendRetrievedContext(contextBuilder, sourceNumber++, match.embeddingId());
            }
        } catch (Exception e) {
            return "";
        }
        return contextBuilder.toString();
    }

    private void appendRetrievedContext(StringBuilder contextBuilder, int sourceNumber, String itemId) {
        Long id = parseId(itemId);
        if (id == null) {
            return;
        }

        Optional<DocumentChunkEntity> chunk = documentChunkRepository.findById(id);
        if (chunk.isPresent()) {
            DocumentChunkEntity entity = chunk.get();
            DocumentEntity document = entity.getDocument();
            contextBuilder
                    .append("[Source ").append(sourceNumber).append("] ")
                    .append(document.getFilename())
                    .append(" #chunk ").append(entity.getChunkIndex())
                    .append("\n")
                    .append(entity.getContent())
                    .append("\n\n");
            return;
        }

        documentRepository.findById(id).ifPresent(document -> contextBuilder
                .append("[Source ").append(sourceNumber).append("] ")
                .append(document.getFilename())
                .append("\n")
                .append("Legacy vector entry points to document metadata only. Re-upload this file to create searchable text chunks.")
                .append("\n\n"));
    }

    private Long parseId(String itemId) {
        try {
            return Long.parseLong(itemId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
