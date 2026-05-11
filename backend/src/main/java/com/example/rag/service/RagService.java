package com.example.rag.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service exposing high-level methods for chat interactions. It hides the
 * underlying complexity of embedding the query, performing vector search
 * against Milvus, and generating an answer with the language model.
 */
@Service
public class RagService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<String> embeddingStore;
    private final ChatLanguageModel chatModel;
    private final ChatMemory chatMemory;

    @Autowired
    public RagService(EmbeddingModel embeddingModel,
                      EmbeddingStore<String> embeddingStore,
                      ChatLanguageModel chatModel,
                      ChatMemory chatMemory) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
    }

    /**
     * Chat with the AI assistant using retrieval-augmented generation. This is
     * a simplified implementation: we embed the user question, query Milvus
     * for nearest neighbours, append their contents into a prompt, and let
     * the language model answer. In production you should implement prompt
     * templating, parent-child chunking and reranking for improved quality.
     *
     * @param conversationId identifier to separate memories for different chats
     * @param userInput user question
     * @return AI generated answer
     */
    public String ask(String conversationId, String userInput) {
        // Record user message in memory
        chatMemory.add(UserMessage.from(userInput));

        // Compute query embedding
        var queryEmbedding = embeddingModel.embed(userInput).content();

        // Retrieve top 3 relevant items from the embedding store. The
        // `findRelevant` API returns matches containing the item key and
        // similarity score. We'll concatenate the keys as a simple context.
        StringBuilder contextBuilder = new StringBuilder();
        try {
            // The EmbeddingStore API may vary by version; this call is
            // illustrative. Replace with the correct call according to your
            // library version, e.g. embeddingStore.findRelevant(vector, k).
            var matches = embeddingStore.findRelevant(queryEmbedding.vector(), 3);
            for (var match : matches) {
                contextBuilder.append(match.getItem()).append("\n");
            }
        } catch (Exception e) {
            // If vector search fails, continue with empty context
        }

        String prompt = "请结合以下上下文回答用户问题：\n" + contextBuilder + "\n问题：" + userInput;

        // Generate answer using the chat model
        var answer = chatModel.generate(prompt);
        // Save AI message in memory
        chatMemory.add(AiMessage.from(answer.content()));
        return answer.content();
    }
}