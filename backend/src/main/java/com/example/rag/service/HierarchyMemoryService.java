package com.example.rag.service;

import com.example.rag.model.DocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HierarchyMemoryService layers conversation history on top of wiki retrieval.
 * A production version can replace this with durable, tenant-scoped memory.
 */
@Service
public class HierarchyMemoryService {

    private final LLMWikiService wikiService;
    private final Map<Long, String> documents = new ConcurrentHashMap<>();
    private final List<String> conversationMemory = new ArrayList<>();

    @Autowired
    public HierarchyMemoryService(LLMWikiService wikiService) {
        this.wikiService = wikiService;
    }

    public void ingest(DocumentEntity document, String content) {
        if (document != null) {
            wikiService.ingest(document, content);
            documents.put(document.getId(), content);
        }
    }

    public void rememberMessage(String message) {
        conversationMemory.add(message);
    }

    public String query(String question) {
        String wiki = wikiService.query(question);
        StringBuilder answer = new StringBuilder(wiki);
        if (!conversationMemory.isEmpty()) {
            answer.append("\n\n[Conversation Memory]\n");
            for (String message : conversationMemory) {
                answer.append(message).append("\n");
            }
        }
        return answer.toString();
    }
}
