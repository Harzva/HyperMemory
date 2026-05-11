package com.example.rag.dto;

/**
 * DTO representing a chat request. Carries the conversation identifier and
 * the user's input message.
 */
public class ChatRequest {
    private String conversationId;
    private String userInput;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }
}