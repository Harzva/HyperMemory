package com.example.rag.service;

import com.example.rag.model.DocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HierarchyMemoryService demonstrates a simplified hierarchy memory model
 * that layers conversation history on top of a persistent wiki. In a
 * production system this would orchestrate retrieval from different
 * layers (vector store, wiki, long‑term memory) and manage the lifecycle
 * of hierarchical memories. Here we provide a minimal implementation
 * combining wiki pages and conversation context.
 */
@Service
public class HierarchyMemoryService {

    private final LLMWikiService wikiService;
    // Store truncated document contents keyed by document ID
    private final Map<Long, String> documents = new ConcurrentHashMap<>();
    // Store conversation messages chronologically
    private final List<String> conversationMemory = new ArrayList<>();

    @Autowired
    public HierarchyMemoryService(LLMWikiService wikiService) {
        this.wikiService = wikiService;
    }

    /**
     * Ingest document content into both the wiki and internal document store.
     *
     * @param document the document entity
     * @param content  the plain text content extracted from the file
     */
    public void ingest(DocumentEntity document, String content) {
        if (document != null) {
            wikiService.ingest(document, content);
            documents.put(document.getId(), content);
        }
    }

    /**
     * Record a message into the conversation memory. This can be used for
     * storing user and assistant messages across turns.
     *
     * @param message the message to remember
     */
    public void rememberMessage(String message) {
        conversationMemory.add(message);
    }

    /**
     * Query the hierarchy memory. Combines wiki pages and conversation
     * history. A real implementation would use the question to determine
     * which layers to consult and how to weight them.
     *
     * @param question the user question
     * @return a textual answer combining wiki and conversation context
     */
    public String query(String question) {
        String wiki = wikiService.query(question);
        StringBuilder sb = new StringBuilder();
        sb.append(wiki);
        if (!conversationMemory.isEmpty()) {
            sb.append("\n\n[Conversation Memory]\n");
            for (String msg : conversationMemory) {
                sb.append(msg).append("\n");
            }
        }
        return sb.toString();
    }
}