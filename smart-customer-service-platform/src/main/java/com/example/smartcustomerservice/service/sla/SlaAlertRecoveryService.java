package com.example.smartcustomerservice.service.sla;

import com.example.smartcustomerservice.domain.vo.SlaAlertRecoveryVO;

// 定义 SLA 告警兜底扫描能力，供定时任务和管理员手动触发共用。
public interface SlaAlertRecoveryService {

    SlaAlertRecoveryVO recoverStaleAlerts();
}
