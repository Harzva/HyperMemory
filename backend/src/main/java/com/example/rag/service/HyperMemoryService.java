package com.example.rag.service;

import com.example.rag.model.DocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * HyperMemoryService provides a compact, bounded memory layer on top of wiki
 * retrieval. Document content stays in the wiki/retrieval layers, while this
 * service keeps recent conversation turns for final response aggregation.
 */
@Service
public class HyperMemoryService {

    private final LLMWikiService wikiService;
    private final Deque<String> conversationMemory = new ConcurrentLinkedDeque<>();
    private final int maxConversationMessages;

    @Autowired
    public HyperMemoryService(LLMWikiService wikiService,
                              @Value("${hypermemory.max-conversation-messages:20}") int maxConversationMessages) {
        this.wikiService = wikiService;
        this.maxConversationMessages = Math.max(1, maxConversationMessages);
    }

    public void ingest(DocumentEntity document, String content) {
        if (document != null) {
            wikiService.ingest(document, content);
        }
    }

    public void rememberMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return;
        }

        conversationMemory.addLast(message.strip());
        trimConversationMemory();
    }

    public String query(String question) {
        String wiki = wikiService.query(question);
        StringBuilder answer = new StringBuilder(wiki);
        List<String> recentMessages = new ArrayList<>(conversationMemory);
        if (!recentMessages.isEmpty()) {
            answer.append("\n\n[Conversation Memory]\n");
            for (String message : recentMessages) {
                answer.append(message).append("\n");
            }
        }
        return answer.toString();
    }

    private void trimConversationMemory() {
        while (conversationMemory.size() > maxConversationMessages) {
            conversationMemory.pollFirst();
        }
    }
}
