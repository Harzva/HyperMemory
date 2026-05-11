package com.example.rag.service;

import com.example.rag.model.DocumentEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple service to demonstrate the concept of a persistent wiki used in an
 * LLM‑Wiki system. In a real LLM‑Wiki implementation, this service would
 * extract facts and relationships from documents, generate cross‑linked wiki
 * pages and maintain them incrementally. For demonstration purposes we
 * merely store truncated text from uploaded documents keyed by their ID and
 * return a concatenation of all wiki pages when queried.
 */
@Service
public class LLMWikiService {

    /**
     * In‑memory storage of wiki pages. The key is the document ID and the
     * value is a text snippet representing the wiki page. In a real system,
     * this would be persisted in a database and include structured fields
     * such as title, frontmatter, body and backlinks.
     */
    private final Map<Long, String> wikiPages = new ConcurrentHashMap<>();

    /**
     * Ingest a document into the wiki. This implementation simply stores the
     * first 500 characters of the document's original filename and object key
     * as a wiki page. If your documents contain textual content, you could
     * load and summarise them here.
     *
     * @param document the metadata record of the uploaded document
     * @param content the plain text content extracted from the uploaded file
     */
    public void ingest(DocumentEntity document, String content) {
        if (document == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(document.getFilename()).append("\n\n");
        if (content != null && !content.isEmpty()) {
            // Truncate the content to avoid huge wiki pages
            int limit = Math.min(content.length(), 500);
            sb.append(content, 0, limit);
            if (content.length() > limit) {
                sb.append("...\n");
            }
        } else {
            sb.append("(no extractable text available)\n");
        }
        wikiPages.put(document.getId(), sb.toString());
    }

    /**
     * Query the wiki by returning the concatenation of all stored wiki pages.
     * A real LLM‑Wiki implementation would perform retrieval over structured
     * pages and perhaps cross‑link or summarise the results.
     *
     * @param question the user question (unused in this toy implementation)
     * @return concatenated wiki page contents
     */
    public String query(String question) {
        return wikiPages.values().stream()
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}