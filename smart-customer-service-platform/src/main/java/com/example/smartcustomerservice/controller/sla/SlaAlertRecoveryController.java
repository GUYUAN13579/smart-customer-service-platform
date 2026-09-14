package com.example.smartcustomerservice.controller.sla;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.vo.SlaAlertRecoveryVO;
import com.example.smartcustomerservice.service.sla.SlaAlertRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SLA告警兜底扫描")
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/sla/alerts/recovery")
// 仅提供管理员手动触发入口；日常恢复由定时任务完成。
public class SlaAlertRecoveryController {

    private final SlaAlertRecoveryService slaAlertRecoveryService;

    public SlaAlertRecoveryController(SlaAlertRecoveryService slaAlertRecoveryService) {
        this.slaAlertRecoveryService = slaAlertRecoveryService;
    }

    @Operation(summary = "手动执行SLA告警兜底扫描")
    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('sla-alert:recovery:scan')")
    public ApiResult<SlaAlertRecoveryVO> recoverStaleAlerts() {
        return ApiResult.success(slaAlertRecoveryService.recoverStaleAlerts());
    }
}
