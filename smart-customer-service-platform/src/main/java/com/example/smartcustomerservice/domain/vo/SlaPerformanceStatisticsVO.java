package com.example.smartcustomerservice.domain.vo;

/** 已进入考核范围的工单 SLA 达标统计。 */
public class SlaPerformanceStatisticsVO {

    private Long firstResponseEvaluatedCount;
    private Long firstResponseMetCount;
    private Long firstResponseBreachedCount;
    private Double firstResponseComplianceRate;
    private Long resolveEvaluatedCount;
    private Long resolveMetCount;
    private Long resolveBreachedCount;
    private Double resolveComplianceRate;

    public Long getFirstResponseEvaluatedCount() { return firstResponseEvaluatedCount; }
    public void setFirstResponseEvaluatedCount(Long firstResponseEvaluatedCount) { this.firstResponseEvaluatedCount = firstResponseEvaluatedCount; }
    public Long getFirstResponseMetCount() { return firstResponseMetCount; }
    public void setFirstResponseMetCount(Long firstResponseMetCount) { this.firstResponseMetCount = firstResponseMetCount; }
    public Long getFirstResponseBreachedCount() { return firstResponseBreachedCount; }
    public void setFirstResponseBreachedCount(Long firstResponseBreachedCount) { this.firstResponseBreachedCount = firstResponseBreachedCount; }
    public Double getFirstResponseComplianceRate() { return firstResponseComplianceRate; }
    public void setFirstResponseComplianceRate(Double firstResponseComplianceRate) { this.firstResponseComplianceRate = firstResponseComplianceRate; }
    public Long getResolveEvaluatedCount() { return resolveEvaluatedCount; }
    public void setResolveEvaluatedCount(Long resolveEvaluatedCount) { this.resolveEvaluatedCount = resolveEvaluatedCount; }
    public Long getResolveMetCount() { return resolveMetCount; }
    public void setResolveMetCount(Long resolveMetCount) { this.resolveMetCount = resolveMetCount; }
    public Long getResolveBreachedCount() { return resolveBreachedCount; }
    public void setResolveBreachedCount(Long resolveBreachedCount) { this.resolveBreachedCount = resolveBreachedCount; }
    public Double getResolveComplianceRate() { return resolveComplianceRate; }
    public void setResolveComplianceRate(Double resolveComplianceRate) { this.resolveComplianceRate = resolveComplianceRate; }
}
