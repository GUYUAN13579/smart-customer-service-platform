package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

// 返回一次 SLA 告警兜底扫描的处理结果，便于管理员手动触发时确认效果。
public class SlaAlertRecoveryVO {

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer pendingRecoveredCount;
    private Integer processingRecoveredCount;
    private Integer publishedCount;

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getPendingRecoveredCount() {
        return pendingRecoveredCount;
    }

    public void setPendingRecoveredCount(Integer pendingRecoveredCount) {
        this.pendingRecoveredCount = pendingRecoveredCount;
    }

    public Integer getProcessingRecoveredCount() {
        return processingRecoveredCount;
    }

    public void setProcessingRecoveredCount(Integer processingRecoveredCount) {
        this.processingRecoveredCount = processingRecoveredCount;
    }

    public Integer getPublishedCount() {
        return publishedCount;
    }

    public void setPublishedCount(Integer publishedCount) {
        this.publishedCount = publishedCount;
    }
}
