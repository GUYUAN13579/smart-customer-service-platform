package com.example.smartcustomerservice.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.prompts")
// AiPromptProperties 属于智能客服平台基础代码。
public class AiPromptProperties {

    private String chatSystem;
    private String sessionSummarySystem;
    private String autoReplySystem;
    private String ticketDraftSystem;

    public String getChatSystem() {
        return chatSystem;
    }

    public void setChatSystem(String chatSystem) {
        this.chatSystem = chatSystem;
    }

    public String getSessionSummarySystem() {
        return sessionSummarySystem;
    }

    public void setSessionSummarySystem(String sessionSummarySystem) {
        this.sessionSummarySystem = sessionSummarySystem;
    }

    public String getAutoReplySystem() {
        return autoReplySystem;
    }

    public void setAutoReplySystem(String autoReplySystem) {
        this.autoReplySystem = autoReplySystem;
    }

    public String getTicketDraftSystem() {
        return ticketDraftSystem;
    }

    public void setTicketDraftSystem(String ticketDraftSystem) {
        this.ticketDraftSystem = ticketDraftSystem;
    }
}
