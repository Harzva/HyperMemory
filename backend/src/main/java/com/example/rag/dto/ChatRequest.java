package com.example.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO representing a chat request. Carries the conversation identifier and
 * the user's input message.
 */
public class ChatRequest {
    @Size(max = 128, message = "conversationId must be at most 128 characters")
    private String conversationId;

    @Size(max = 128, message = "tenantId must be at most 128 characters")
    private String tenantId;

    @NotBlank(message = "userInput is required")
    @Size(max = 4000, message = "userInput must be at most 4000 characters")
    private String userInput;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }
}
