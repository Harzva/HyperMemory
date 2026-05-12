package com.example.rag.service;

import com.example.rag.model.DocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HyperMemoryService provides a compact hyper-memory layer on top of wiki
 * retrieval. It stores document contents, keeps conversation history, and
 * combines both when answering questions.
 */
@Service
public class HyperMemoryService {

    private final LLMWikiService wikiService;
    private final Map<Long, String> documents = new ConcurrentHashMap<>();
    private final List<String> conversationMemory = new ArrayList<>();

    @Autowired
    public HyperMemoryService(LLMWikiService wikiService) {
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
