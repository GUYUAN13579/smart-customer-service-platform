package com.example.smartcustomerservice.controller.sla;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SlaTicketMonitorQueryRequest;
import com.example.smartcustomerservice.domain.dto.SlaTicketRemindRequest;
import com.example.smartcustomerservice.domain.vo.SlaTicketMonitorVO;
import com.example.smartcustomerservice.domain.vo.SlaTicketRemindVO;
import com.example.smartcustomerservice.service.sla.SlaMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SLA监控")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/sla/tickets")
// SlaMonitorController 属于智能客服平台基础代码。
public class SlaMonitorController {

    private final SlaMonitorService slaMonitorService;

    public SlaMonitorController(SlaMonitorService slaMonitorService) {
        this.slaMonitorService = slaMonitorService;
    }

    @Operation(summary = "即将超时工单分页查询")
    @GetMapping("/risk")
    @PreAuthorize("hasAuthority('sla-monitor:risk')")
    public ApiResult<PageResult<SlaTicketMonitorVO>> pageRiskTickets(@Valid SlaTicketMonitorQueryRequest request) {
        return ApiResult.success(slaMonitorService.pageRiskTickets(request));
    }

    @Operation(summary = "已超时工单分页查询")
    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('sla-monitor:overdue')")
    public ApiResult<PageResult<SlaTicketMonitorVO>> pageOverdueTickets(@Valid SlaTicketMonitorQueryRequest request) {
        return ApiResult.success(slaMonitorService.pageOverdueTickets(request));
    }

    @Operation(summary = "手动发送SLA提醒")
    @PostMapping("/{ticketId}/remind")
    @PreAuthorize("hasAuthority('sla-monitor:remind')")
    public ApiResult<SlaTicketRemindVO> remindTicket(
            @NotNull(message = "工单ID不能为空") @PathVariable Long ticketId,
            @Valid @RequestBody SlaTicketRemindRequest request) {
        return ApiResult.success(slaMonitorService.remindTicket(ticketId, request));
    }
}
