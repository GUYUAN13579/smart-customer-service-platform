package com.example.smartcustomerservice.controller.sla;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SlaAlertQueryRequest;
import com.example.smartcustomerservice.domain.vo.SlaAlertVO;
import com.example.smartcustomerservice.service.sla.SlaAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SLA自动告警")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/sla/alerts")
// SlaAlertController 属于智能客服平台基础代码。
public class SlaAlertController {

    private final SlaAlertService slaAlertService;

    public SlaAlertController(SlaAlertService slaAlertService) {
        this.slaAlertService = slaAlertService;
    }

    @Operation(summary = "SLA自动告警分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('sla-alert:list')")
    public ApiResult<PageResult<SlaAlertVO>> pageAlerts(@Valid SlaAlertQueryRequest request) {
        return ApiResult.success(slaAlertService.pageAlerts(request));
    }

    @Operation(summary = "SLA自动告警详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sla-alert:detail')")
    public ApiResult<SlaAlertVO> getAlert(@NotNull(message = "SLA告警ID不能为空") @PathVariable Long id) {
        return ApiResult.success(slaAlertService.getAlert(id));
    }

    @Operation(summary = "重试失败的SLA自动告警")
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('sla-alert:retry')")
    public ApiResult<SlaAlertVO> retryAlert(@NotNull(message = "SLA告警ID不能为空") @PathVariable Long id) {
        return ApiResult.success(slaAlertService.retryAlert(id));
    }
}
