package com.example.smartcustomerservice.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sla.alert-recovery")
// 集中管理 SLA 告警兜底扫描的频率、宽限时间和单次处理上限。
public class SlaAlertRecoveryProperties {

    private long fixedDelayMs = 60000L;
    private long pendingGraceMinutes = 5L;
    private long processingTimeoutMinutes = 10L;
    private int batchSize = 100;

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public long getPendingGraceMinutes() {
        return pendingGraceMinutes;
    }

    public void setPendingGraceMinutes(long pendingGraceMinutes) {
        this.pendingGraceMinutes = pendingGraceMinutes;
    }

    public long getProcessingTimeoutMinutes() {
        return processingTimeoutMinutes;
    }

    public void setProcessingTimeoutMinutes(long processingTimeoutMinutes) {
        this.processingTimeoutMinutes = processingTimeoutMinutes;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
