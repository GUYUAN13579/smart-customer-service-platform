package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDate;

/** 单日工单生命周期趋势数据。 */
public class TicketTrendStatisticsVO {

    private LocalDate statDate;
    private Long createdTicketCount;
    private Long resolvedTicketCount;
    private Long closedTicketCount;

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Long getCreatedTicketCount() { return createdTicketCount; }
    public void setCreatedTicketCount(Long createdTicketCount) { this.createdTicketCount = createdTicketCount; }
    public Long getResolvedTicketCount() { return resolvedTicketCount; }
    public void setResolvedTicketCount(Long resolvedTicketCount) { this.resolvedTicketCount = resolvedTicketCount; }
    public Long getClosedTicketCount() { return closedTicketCount; }
    public void setClosedTicketCount(Long closedTicketCount) { this.closedTicketCount = closedTicketCount; }
}
