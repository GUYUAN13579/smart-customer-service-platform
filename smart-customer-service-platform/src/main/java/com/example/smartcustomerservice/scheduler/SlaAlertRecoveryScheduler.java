package com.example.smartcustomerservice.scheduler;

import com.example.smartcustomerservice.service.sla.SlaAlertRecoveryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
// 按配置周期触发 SLA 告警兜底扫描，避免消息投递或消费者异常导致告警长期滞留。
public class SlaAlertRecoveryScheduler {

    private final SlaAlertRecoveryService slaAlertRecoveryService;

    public SlaAlertRecoveryScheduler(SlaAlertRecoveryService slaAlertRecoveryService) {
        this.slaAlertRecoveryService = slaAlertRecoveryService;
    }

    @Scheduled(fixedDelayString = "${app.sla.alert-recovery.fixed-delay-ms:60000}")
    public void recoverStaleAlerts() {
        slaAlertRecoveryService.recoverStaleAlerts();
    }
}
