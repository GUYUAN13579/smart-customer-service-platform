package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDate;

// DashboardStatisticsVO 用于运营看板的聚合统计结果。
public class DashboardStatisticsVO {

    private LocalDate statDate;
    private Long ticketCount;
    private Long closedCount;
    private Long todaySessionCount;
    private Long todayTicketCount;
    private Long pendingReviewTicketCount;
    private Long waitingAssignTicketCount;
    private Long processingTicketCount;
    private Long riskTicketCount;
    private Long overdueTicketCount;
    private Double avgFirstResponseMinutes;
    private Double aiResolveRate;

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Long getTicketCount() { return ticketCount; }
    public void setTicketCount(Long ticketCount) { this.ticketCount = ticketCount; }
    public Long getClosedCount() { return closedCount; }
    public void setClosedCount(Long closedCount) { this.closedCount = closedCount; }
    public Long getTodaySessionCount() { return todaySessionCount; }
    public void setTodaySessionCount(Long todaySessionCount) { this.todaySessionCount = todaySessionCount; }
    public Long getTodayTicketCount() { return todayTicketCount; }
    public void setTodayTicketCount(Long todayTicketCount) { this.todayTicketCount = todayTicketCount; }
    public Long getPendingReviewTicketCount() { return pendingReviewTicketCount; }
    public void setPendingReviewTicketCount(Long pendingReviewTicketCount) { this.pendingReviewTicketCount = pendingReviewTicketCount; }
    public Long getWaitingAssignTicketCount() { return waitingAssignTicketCount; }
    public void setWaitingAssignTicketCount(Long waitingAssignTicketCount) { this.waitingAssignTicketCount = waitingAssignTicketCount; }
    public Long getProcessingTicketCount() { return processingTicketCount; }
    public void setProcessingTicketCount(Long processingTicketCount) { this.processingTicketCount = processingTicketCount; }
    public Long getRiskTicketCount() { return riskTicketCount; }
    public void setRiskTicketCount(Long riskTicketCount) { this.riskTicketCount = riskTicketCount; }
    public Long getOverdueTicketCount() { return overdueTicketCount; }
    public void setOverdueTicketCount(Long overdueTicketCount) { this.overdueTicketCount = overdueTicketCount; }
    public Double getAvgFirstResponseMinutes() { return avgFirstResponseMinutes; }
    public void setAvgFirstResponseMinutes(Double avgFirstResponseMinutes) { this.avgFirstResponseMinutes = avgFirstResponseMinutes; }
    public Double getAiResolveRate() { return aiResolveRate; }
    public void setAiResolveRate(Double aiResolveRate) { this.aiResolveRate = aiResolveRate; }
}
