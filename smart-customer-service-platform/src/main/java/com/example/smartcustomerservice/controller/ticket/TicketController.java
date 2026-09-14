package com.example.smartcustomerservice.controller.ticket;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.TicketAssignRequest;
import com.example.smartcustomerservice.domain.dto.TicketApplySlaRequest;
import com.example.smartcustomerservice.domain.dto.TicketCloseRequest;
import com.example.smartcustomerservice.domain.dto.TicketOperationLogQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketProcessRecordCreateRequest;
import com.example.smartcustomerservice.domain.dto.TicketProcessRecordQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketReviewRequest;
import com.example.smartcustomerservice.domain.dto.TicketResolveRequest;
import com.example.smartcustomerservice.domain.dto.TicketStartRequest;
import com.example.smartcustomerservice.domain.vo.TicketApplySlaVO;
import com.example.smartcustomerservice.domain.vo.TicketAutoAssignPreviewVO;
import com.example.smartcustomerservice.domain.vo.TicketFullDetailVO;
import com.example.smartcustomerservice.domain.vo.TicketOperationLogVO;
import com.example.smartcustomerservice.domain.vo.TicketProcessRecordVO;
import com.example.smartcustomerservice.domain.vo.TicketVO;
import com.example.smartcustomerservice.service.ticket.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工单管理")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/tickets")
// TicketController 属于智能客服平台基础代码。
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "待审核工单分页查询")
    @GetMapping("/pending-review")
    @PreAuthorize("hasAuthority('ticket:pending-review')")
    public ApiResult<PageResult<TicketVO>> pagePendingReviewTickets(@Valid TicketQueryRequest request) {
        return ApiResult.success(ticketService.pagePendingReviewTickets(request));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ticket:detail')")
    public ApiResult<TicketVO> getTicketDetail(@NotNull(message = "工单ID不能为空") @PathVariable Long id) {
        return ApiResult.success(ticketService.getTicketDetail(id));
    }

    @Operation(summary = "工单聚合详情")
    @GetMapping("/{id}/full-detail")
    @PreAuthorize("hasAuthority('ticket:full-detail')")
    public ApiResult<TicketFullDetailVO> getTicketFullDetail(@NotNull(message = "工单ID不能为空") @PathVariable Long id) {
        return ApiResult.success(ticketService.getTicketFullDetail(id));
    }

    @Operation(summary = "审核工单")
    @PutMapping("/{id}/review")
    @PreAuthorize("hasAuthority('ticket:review')")
    public ApiResult<TicketVO> reviewTicket(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                            @Valid @RequestBody TicketReviewRequest request) {
        return ApiResult.success(ticketService.reviewTicket(id, request));
    }

    @Operation(summary = "待派单工单分页查询")
    @GetMapping("/waiting-assign")
    @PreAuthorize("hasAuthority('ticket:waiting-assign')")
    public ApiResult<PageResult<TicketVO>> pageWaitingAssignTickets(@Valid TicketQueryRequest request) {
        return ApiResult.success(ticketService.pageWaitingAssignTickets(request));
    }

    @Operation(summary = "派单")
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('ticket:assign')")
    public ApiResult<TicketVO> assignTicket(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                            @Valid @RequestBody TicketAssignRequest request) {
        return ApiResult.success(ticketService.assignTicket(id, request));
    }

    @Operation(summary = "自动派单预览")
    @GetMapping("/{id}/assignment-preview")
    @PreAuthorize("hasAuthority('ticket:auto-assign:preview')")
    public ApiResult<TicketAutoAssignPreviewVO> previewAutoAssign(@NotNull(message = "工单ID不能为空") @PathVariable Long id) {
        return ApiResult.success(ticketService.previewAutoAssign(id));
    }

    @Operation(summary = "自动派单")
    @PostMapping("/{id}/auto-assign")
    @PreAuthorize("hasAuthority('ticket:auto-assign')")
    public ApiResult<TicketVO> autoAssignTicket(@NotNull(message = "工单ID不能为空") @PathVariable Long id) {
        return ApiResult.success(ticketService.autoAssignTicket(id));
    }

    @Operation(summary = "我的工单分页查询")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ticket:my')")
    public ApiResult<PageResult<TicketVO>> pageMyTickets(@Valid TicketQueryRequest request) {
        return ApiResult.success(ticketService.pageMyTickets(request));
    }

    @Operation(summary = "开始处理工单")
    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('ticket:start')")
    public ApiResult<TicketVO> startTicket(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                           @Valid @RequestBody(required = false) TicketStartRequest request) {
        return ApiResult.success(ticketService.startTicket(id, request));
    }

    @Operation(summary = "新增工单处理记录")
    @PostMapping("/{id}/records")
    @PreAuthorize("hasAuthority('ticket:record:add')")
    public ApiResult<TicketProcessRecordVO> addTicketProcessRecord(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                                                   @Valid @RequestBody TicketProcessRecordCreateRequest request) {
        return ApiResult.success(ticketService.addTicketProcessRecord(id, request));
    }

    @Operation(summary = "工单处理记录分页查询")
    @GetMapping("/{id}/records")
    @PreAuthorize("hasAuthority('ticket:record:list')")
    public ApiResult<PageResult<TicketProcessRecordVO>> pageTicketProcessRecords(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                                                                 @Valid TicketProcessRecordQueryRequest request) {
        return ApiResult.success(ticketService.pageTicketProcessRecords(id, request));
    }

    @Operation(summary = "工单操作日志分页查询")
    @GetMapping("/{id}/operation-logs")
    @PreAuthorize("hasAuthority('ticket:operation-log:list')")
    public ApiResult<PageResult<TicketOperationLogVO>> pageTicketOperationLogs(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                                                               @Valid TicketOperationLogQueryRequest request) {
        return ApiResult.success(ticketService.pageTicketOperationLogs(id, request));
    }

    @Operation(summary = "解决工单")
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('ticket:resolve')")
    public ApiResult<TicketVO> resolveTicket(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                             @Valid @RequestBody TicketResolveRequest request) {
        return ApiResult.success(ticketService.resolveTicket(id, request));
    }

    @Operation(summary = "关闭工单")
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('ticket:close')")
    public ApiResult<TicketVO> closeTicket(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                           @Valid @RequestBody TicketCloseRequest request) {
        return ApiResult.success(ticketService.closeTicket(id, request));
    }

    @Operation(summary = "应用SLA策略")
    @PostMapping("/{id}/apply-sla")
    @PreAuthorize("hasAuthority('ticket:apply-sla')")
    public ApiResult<TicketApplySlaVO> applySla(@NotNull(message = "工单ID不能为空") @PathVariable Long id,
                                                @Valid @RequestBody(required = false) TicketApplySlaRequest request) {
        return ApiResult.success(ticketService.applySla(id, request));
    }
}
