package com.example.smartcustomerservice.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.knowledge.index")
public class KnowledgeIndexProperties {

    private String indexName = "knowledge_chunk_v1";
    private Integer vectorDimensions = 1024;
    private boolean autoRetryEnabled = true;
    private long autoRetryInitialDelayMs = 60_000L;
    private long autoRetryMaxDelayMs = 900_000L;

    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    public Integer getVectorDimensions() { return vectorDimensions; }
    public void setVectorDimensions(Integer vectorDimensions) { this.vectorDimensions = vectorDimensions; }
    public boolean isAutoRetryEnabled() { return autoRetryEnabled; }
    public void setAutoRetryEnabled(boolean autoRetryEnabled) { this.autoRetryEnabled = autoRetryEnabled; }
    public long getAutoRetryInitialDelayMs() { return autoRetryInitialDelayMs; }
    public void setAutoRetryInitialDelayMs(long autoRetryInitialDelayMs) { this.autoRetryInitialDelayMs = autoRetryInitialDelayMs; }
    public long getAutoRetryMaxDelayMs() { return autoRetryMaxDelayMs; }
    public void setAutoRetryMaxDelayMs(long autoRetryMaxDelayMs) { this.autoRetryMaxDelayMs = autoRetryMaxDelayMs; }
}
