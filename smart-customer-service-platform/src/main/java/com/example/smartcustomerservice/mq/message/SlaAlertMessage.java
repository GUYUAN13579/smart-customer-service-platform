package com.example.smartcustomerservice.mq.message;

import java.io.Serial;
import java.io.Serializable;

// SlaAlertMessage 属于智能客服平台基础代码。
public class SlaAlertMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long alertId;

    public SlaAlertMessage() {
    }

    public SlaAlertMessage(Long alertId) {
        this.alertId = alertId;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }
}
