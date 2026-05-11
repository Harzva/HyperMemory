package com.example.rag.service;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Agent-based QA service. Agent tools now use the same chunk hydration path as
 * the regular RAG endpoint, so tool retrieval returns real source text instead
 * of Milvus item IDs.
 */
@Service
public class AgentService {

    private final CampusAssistant assistant;

    public AgentService(ChatModel chatModel,
                        RetrievalContextService retrievalContextService) {
        this.assistant = AiServices.builder(CampusAssistant.class)
                .chatModel(chatModel)
                .systemMessage("""
                        You are a campus QA agent. Use tools when a user asks about campus knowledge,
                        documents, schedules, or facts that may exist in the knowledge base. Prefer
                        grounded retrieved context over guesses.
                        """)
                .tools(new CampusTools(retrievalContextService))
                .build();
    }

    public String ask(String question) {
        return assistant.chat(question);
    }

    private interface CampusAssistant {
        String chat(String question);
    }

    public static class CampusTools {
        private static final int TOOL_TOP_K = 5;

        private final RetrievalContextService retrievalContextService;

        public CampusTools(RetrievalContextService retrievalContextService) {
            this.retrievalContextService = retrievalContextService;
        }

        @Tool("Get the current time for the user")
        public String currentTime() {
            return LocalDateTime.now().toString();
        }

        @Tool("FAQ lookup; returns an answer if a known campus question is asked")
        public String faq(String question) {
            String lower = question.toLowerCase();
            if (lower.contains("exam")) {
                return "The exam schedule will be released next week.";
            }
            if (lower.contains("holiday") || lower.contains("vacation")) {
                return "School holidays are from June 1 to June 15.";
            }
            return "I don't have a FAQ answer for that yet.";
        }

        @Tool("Retrieve grounded source text from the knowledge base")
        public String retrieveContext(String query) {
            String context = retrievalContextService.retrieveContext(query, TOOL_TOP_K);
            return context.isBlank()
                    ? "No relevant knowledge chunks were retrieved."
                    : context;
        }
    }
}
