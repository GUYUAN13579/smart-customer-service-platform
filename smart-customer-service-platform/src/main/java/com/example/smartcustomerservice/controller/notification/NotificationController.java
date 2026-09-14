package com.example.smartcustomerservice.controller.notification;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.NotificationQueryRequest;
import com.example.smartcustomerservice.domain.vo.NotificationUnreadCountVO;
import com.example.smartcustomerservice.domain.vo.NotificationVO;
import com.example.smartcustomerservice.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "通知中心")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/notifications")
// NotificationController 属于智能客服平台基础代码。
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "当前用户通知分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('notification:list')")
    public ApiResult<PageResult<NotificationVO>> pageNotifications(@Valid NotificationQueryRequest request) {
        return ApiResult.success(notificationService.pageNotifications(request));
    }

    @Operation(summary = "查询当前用户未读通知数")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('notification:unread-count')")
    public ApiResult<NotificationUnreadCountVO> getUnreadCount() {
        return ApiResult.success(notificationService.getUnreadCount());
    }

    @Operation(summary = "将指定通知标记为已读")
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification:read')")
    public ApiResult<Boolean> markAsRead(@NotNull(message = "通知ID不能为空") @PathVariable Long id) {
        return ApiResult.success(notificationService.markAsRead(id));
    }

    @Operation(summary = "将当前用户全部通知标记为已读")
    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('notification:read-all')")
    public ApiResult<Boolean> markAllAsRead() {
        return ApiResult.success(notificationService.markAllAsRead());
    }
}
