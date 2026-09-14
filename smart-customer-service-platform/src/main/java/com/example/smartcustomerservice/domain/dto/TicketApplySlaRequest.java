package com.example.smartcustomerservice.domain.dto;

import java.time.LocalDateTime;

// TicketApplySlaRequest 属于智能客服平台基础代码。
public class TicketApplySlaRequest {

    private LocalDateTime baseTime;
    private Boolean overwrite;

    public LocalDateTime getBaseTime() {
        return baseTime;
    }

    public void setBaseTime(LocalDateTime baseTime) {
        this.baseTime = baseTime;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }
}
