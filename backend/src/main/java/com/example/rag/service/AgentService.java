package com.example.rag.service;

import dev.langchain4j.agent.Agent;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service providing agent‑based question answering. This implementation
 * constructs a LangChain4j {@link Agent} that can call annotated tool
 * methods to fulfill user requests. The agent uses the same chat model
 * configured for the RAG pipeline, so no additional model setup is
 * required. Tools are defined as inner classes and annotated with
 * {@code @Tool} to signal to LangChain4j that they may be invoked by
 * the agent. See the {@code CampusTools} class below for examples.
 */
@Service
public class AgentService {

    private final Agent agent;

    /**
     * Construct a new {@code AgentService}. It builds a LangChain4j Agent
     * instance backed by the provided chat model and registers a set
     * of tools that the agent may call when reasoning about a user
     * request.
     *
     * @param chatModel the underlying large language model used for
     *                  natural language generation
     */
    public AgentService(ChatLanguageModel chatModel, dev.langchain4j.model.embedding.EmbeddingModel embeddingModel,
                        dev.langchain4j.store.embedding.EmbeddingStore<String> embeddingStore) {
        // Build an agent that uses the provided chat model and registers a set of tools.
        // We pass the embedding model and store to our tools so they can perform retrieval.
        this.agent = Agent.builder()
                .model(chatModel)
                .tools(List.of(new CampusTools(embeddingModel, embeddingStore)))
                .build();
    }

    /**
     * Ask the agent a question. The agent may choose to call one or
     * more tools registered with it during reasoning. If the agent
     * decides to answer directly, its response will be returned.
     *
     * @param question the user's question
     * @return the agent's answer
     */
    public String ask(String question) {
        return agent.chat(question);
    }

    /**
     * Collection of helper functions exposed to the agent as tools.
     * Annotated methods can be invoked by the agent during problem
     * solving. Each tool should include a succinct description of
     * its purpose in the annotation value. For demonstration
     * purposes we provide a time tool and a simple FAQ lookup.
     */
    public static class CampusTools {
        private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;
        private final dev.langchain4j.store.embedding.EmbeddingStore<String> embeddingStore;

        /**
         * Construct tools with access to the embedding model and store. These
         * dependencies enable retrieval of relevant context from the RAG
         * knowledge base when answering questions.
         *
         * @param embeddingModel the model used to compute query embeddings
         * @param embeddingStore the store used to search for similar documents
         */
        public CampusTools(dev.langchain4j.model.embedding.EmbeddingModel embeddingModel,
                           dev.langchain4j.store.embedding.EmbeddingStore<String> embeddingStore) {
            this.embeddingModel = embeddingModel;
            this.embeddingStore = embeddingStore;
        }

        /**
         * Return the current date and time in ISO format. The agent
         * can call this tool when a user asks about the present
         * time.
         *
         * @return current timestamp as an ISO‑8601 string
         */
        @Tool("Get the current time for the user")
        public String currentTime() {
            return LocalDateTime.now().toString();
        }

        /**
         * Perform a simple FAQ lookup. If the question matches
         * predefined keywords, return a canned answer. Otherwise,
         * return a default message.
         *
         * @param question the user question
         * @return the FAQ answer or a fallback response
         */
        @Tool("FAQ lookup; returns an answer if a known question is asked")
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

        /**
         * Retrieve relevant context from the knowledge base by embedding
         * the query and performing a vector search. The returned
         * context can then be used by the language model to answer
         * questions more accurately. This tool provides an explicit
         * mechanism for the agent to access the RAG pipeline.
         *
         * @param query the user question
         * @return concatenated contents of the most relevant items
         */
        @Tool("Retrieve relevant context from the knowledge base")
        public String retrieveContext(String query) {
            try {
                var queryEmbedding = embeddingModel.embed(query).content();
                var matches = embeddingStore.findRelevant(queryEmbedding.vector(), 3);
                StringBuilder context = new StringBuilder();
                for (var match : matches) {
                    context.append(match.getItem()).append("\n");
                }
                return context.toString();
            } catch (Exception e) {
                return "";
            }
        }
    }
}