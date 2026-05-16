package com.example.rag.service;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Agent-based QA service. Agent tools now use the same chunk hydration path as
 * the regular RAG endpoint, so tool retrieval returns real source text instead
 * of Milvus item IDs.
 */
@Service
public class AgentService {

    private final CampusAssistant assistant;
    private final CampusTools campusTools;

    public AgentService(ChatModel chatModel,
                        RetrievalContextService retrievalContextService) {
        this.campusTools = new CampusTools(retrievalContextService);
        this.assistant = AiServices.builder(CampusAssistant.class)
                .chatModel(chatModel)
                .systemMessage("""
                        You are a campus QA agent. Use the retrieval tool when a user asks about
                        campus knowledge, documents, schedules, or facts that may exist in the
                        knowledge base. Prefer grounded retrieved context over guesses.
                        """)
                .tools(campusTools)
                .build();
    }

    public String ask(String question) {
        return ask(question, null);
    }

    public String ask(String question, String tenantId) {
        campusTools.setTenantId(tenantId);
        try {
            return assistant.chat(question);
        } finally {
            campusTools.clearTenantId();
        }
    }

    private interface CampusAssistant {
        String chat(String question);
    }

    public static class CampusTools {
        private static final int TOOL_TOP_K = 5;

        private final RetrievalContextService retrievalContextService;
        private final ThreadLocal<String> tenantId = ThreadLocal.withInitial(() -> "default");

        public CampusTools(RetrievalContextService retrievalContextService) {
            this.retrievalContextService = retrievalContextService;
        }

        public void setTenantId(String tenantId) {
            this.tenantId.set(normalizeTenantId(tenantId));
        }

        public void clearTenantId() {
            tenantId.remove();
        }

        @Tool("Get the current time for the user")
        public String currentTime() {
            return LocalDateTime.now().toString();
        }

        @Tool("Retrieve grounded source text from the knowledge base")
        public String retrieveContext(String query) {
            String context = retrievalContextService.retrieveContext(query, TOOL_TOP_K, tenantId.get());
            return context.isBlank()
                    ? "No relevant knowledge chunks were retrieved."
                    : context;
        }

        private String normalizeTenantId(String value) {
            return value == null || value.isBlank()
                    ? "default"
                    : value.trim().toLowerCase(Locale.ROOT);
        }
    }
}
