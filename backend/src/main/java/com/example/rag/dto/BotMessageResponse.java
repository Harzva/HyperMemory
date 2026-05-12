package com.example.rag.dto;

public class BotMessageResponse {
    private boolean ok;
    private String channel;
    private String conversationId;
    private String mode;
    private String answer;
    private String error;

    public static BotMessageResponse success(String channel, String conversationId, String mode, String answer) {
        BotMessageResponse response = new BotMessageResponse();
        response.ok = true;
        response.channel = channel;
        response.conversationId = conversationId;
        response.mode = mode;
        response.answer = answer;
        return response;
    }

    public static BotMessageResponse failure(String channel, String conversationId, String mode, String error) {
        BotMessageResponse response = new BotMessageResponse();
        response.ok = false;
        response.channel = channel;
        response.conversationId = conversationId;
        response.mode = mode;
        response.error = error;
        return response;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
