package com.example.rag.service;

import com.example.rag.model.DocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HyperMemoryService provides a simplified "hyper" memory model built
 * atop the existing LLM‑Wiki service.  Similar to the hierarchy memory
 * example, it stores document contents, maintains a conversation history
 * and synthesises answers by combining wiki retrieval with the stored
 * conversation context.  In a real system the hyper memory could
 * implement more advanced aggregation and long‑term memory strategies.
 */
@Service
public class HyperMemoryService {

    private final LLMWikiService wikiService;
    // Store truncated document contents keyed by document ID
    private final Map<Long, String> documents = new ConcurrentHashMap<>();
    // Store conversation messages chronologically
    private final List<String> conversationMemory = new ArrayList<>();

    @Autowired
    public HyperMemoryService(LLMWikiService wikiService) {
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
     * Record a message into the conversation memory.  This can be used for
     * storing user and assistant messages across turns.
     *
     * @param message the message to remember
     */
    public void rememberMessage(String message) {
        conversationMemory.add(message);
    }

    /**
     * Query the hyper memory.  Combines wiki pages and conversation
     * history.  A real implementation might aggregate or summarise the
     * conversation context; here we simply append it to the wiki output.
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