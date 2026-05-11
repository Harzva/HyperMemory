package com.example.rag.config;

import dev.langchain4j.memory.chat.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore.Builder;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.MetricType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that wires together LangChain4j components. Here we
 * instantiate the embedding model, chat model, chat memory and the Milvus
 * embedding store. By externalising connection details into application
 * properties, you can switch between hosted and local LLM providers without
 * recompiling the application.
 */
@Configuration
public class RagConfig {

    @Value("${openai.api-key:YOUR_OPENAI_KEY}")
    private String openAiApiKey;

    @Value("${milvus.host}")
    private String milvusHost;

    @Value("${milvus.port}")
    private int milvusPort;

    @Value("${milvus.collection-name:rag_collection}")
    private String milvusCollectionName;

    @Value("${milvus.embedding-dimension:768}")
    private int embeddingDimension;

    /**
     * Chat model backed by OpenAI. LangChain4j hides the REST client details and
     * provides a unified API over the provider. For local deployment you can
     * substitute this with the Ollama module simply by changing configuration
     * and dependency.
     */
    @Bean
    public ChatLanguageModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .temperature(0.3)
                .build();
    }

    /**
     * Embedding model used to convert text into vector representations. This
     * example uses OpenAI embeddings but you can switch to HuggingFace or
     * another provider supported by LangChain4j by changing the builder.
     */
    @Bean
    public OpenAiEmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(openAiApiKey)
                .build();
    }

    /**
     * Chat memory that stores recent conversation turns. We use a sliding
     * window memory with a fixed size to limit token usage. You could swap
     * this for a Redis-backed memory using the langchain4j-redis module for
     * persistence across multiple instances.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }

    /**
     * Instantiate the Milvus embedding store. The store communicates with a
     * Milvus server over gRPC and stores vectors for semantic search. The
     * collection name and embedding dimension must match the values used
     * during ingestion.
     */
    @Bean
    public EmbeddingStore<String> milvusEmbeddingStore() {
        Builder<String> builder = MilvusEmbeddingStore.builder();
        builder.host(milvusHost);
        builder.port(milvusPort);
        builder.collectionName(milvusCollectionName);
        builder.dimension(embeddingDimension);
        builder.metricType(MetricType.L2);
        return builder.build();
    }
}