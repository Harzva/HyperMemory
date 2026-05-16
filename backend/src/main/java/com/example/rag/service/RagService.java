package com.example.rag.service;

import com.example.rag.dto.AnswerWithSources;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * High-level RAG service. It retrieves grounded chunk text through
 * RetrievalContextService, then asks the model to answer only from that
 * context.
 */
@Service
public class RagService {

    private static final int TOP_K = 5;

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final RetrievalContextService retrievalContextService;

    @Autowired
    public RagService(ChatModel chatModel,
                      ChatMemory chatMemory,
                      RetrievalContextService retrievalContextService) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.retrievalContextService = retrievalContextService;
    }

    public String ask(String conversationId, String userInput) {
        return askWithSources(conversationId, userInput).getAnswer();
    }

    public String ask(String conversationId, String userInput, String tenantId) {
        return askWithSources(conversationId, userInput, tenantId).getAnswer();
    }

    public AnswerWithSources askWithSources(String conversationId, String userInput) {
        return askWithSources(conversationId, userInput, null);
    }

    public AnswerWithSources askWithSources(String conversationId, String userInput, String tenantId) {
        chatMemory.add(UserMessage.from(userInput));

        RetrievalContextService.RetrievalResult result = retrievalContextService.retrieve(userInput, TOP_K, tenantId);
        String context = result.getFormattedContext().isBlank()
                ? "No relevant knowledge chunks were retrieved."
                : result.getFormattedContext();

        String prompt = """
                Answer the user question using only the retrieved knowledge chunks below.
                If the chunks do not contain enough evidence, say what is missing and do not fabricate.

                Retrieved knowledge chunks:
                %s

                User question:
                %s
                """.formatted(context, userInput);

        String answer = chatModel.chat(prompt);
        chatMemory.add(AiMessage.from(answer));
        return AnswerWithSources.of(answer, result.getCitations());
    }
}
