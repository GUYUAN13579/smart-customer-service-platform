package com.example.smartcustomerservice.controller.conversation;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.ConversationCloseRequest;
import com.example.smartcustomerservice.domain.dto.ConversationCreateRequest;
import com.example.smartcustomerservice.domain.dto.ConversationMessageCreateRequest;
import com.example.smartcustomerservice.domain.dto.ConversationQueryRequest;
import com.example.smartcustomerservice.domain.dto.ConversationTakeOverRequest;
import com.example.smartcustomerservice.domain.vo.ConversationMessageVO;
import com.example.smartcustomerservice.domain.vo.ConversationSessionVO;
import com.example.smartcustomerservice.service.conversation.ConversationService;
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

import java.util.List;

@Tag(name = "会话中心")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(summary = "创建会话")
    @PostMapping
    @PreAuthorize("hasAuthority('conversation:create')")
    public ApiResult<ConversationSessionVO> createSession(@Valid @RequestBody ConversationCreateRequest request) {
        return ApiResult.success(conversationService.createSession(request));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('conversation:detail')")
    public ApiResult<ConversationSessionVO> getSession(@NotNull(message = "会话ID不能为空") @PathVariable Long id) {
        return ApiResult.success(conversationService.getSession(id));
    }

    @Operation(summary = "会话分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('conversation:list')")
    public ApiResult<PageResult<ConversationSessionVO>> pageSessions(@Valid ConversationQueryRequest request) {
        return ApiResult.success(conversationService.pageSessions(request));
    }

    @Operation(summary = "人工接管会话")
    @PostMapping("/{id}/take-over")
    @PreAuthorize("hasAuthority('conversation:takeover')")
    public ApiResult<ConversationSessionVO> takeOverSession(@NotNull(message = "会话ID不能为空") @PathVariable Long id,
                                                            @RequestBody(required = false) ConversationTakeOverRequest request) {
        return ApiResult.success(conversationService.takeOverSession(id, request));
    }

    @Operation(summary = "退出接管会话")
    @PostMapping("/{id}/release-take-over")
    @PreAuthorize("hasAnyAuthority('conversation:release', 'conversation:takeover')")
    public ApiResult<ConversationSessionVO> releaseTakeOverSession(@NotNull(message = "会话ID不能为空") @PathVariable Long id) {
        return ApiResult.success(conversationService.releaseTakeOverSession(id));
    }

    @Operation(summary = "关闭会话")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('conversation:close')")
    public ApiResult<ConversationSessionVO> closeSession(@NotNull(message = "会话ID不能为空") @PathVariable Long id,
                                                         @Valid @RequestBody(required = false) ConversationCloseRequest request) {
        return ApiResult.success(conversationService.closeSession(id, request));
    }

    @Operation(summary = "会话消息列表")
    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('conversation:message:list')")
    public ApiResult<List<ConversationMessageVO>> listMessages(@NotNull(message = "会话ID不能为空") @PathVariable Long id) {
        return ApiResult.success(conversationService.listMessages(id));
    }

    @Operation(summary = "发送会话消息")
    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('conversation:message:send')")
    public ApiResult<ConversationMessageVO> sendMessage(@NotNull(message = "会话ID不能为空") @PathVariable Long id,
                                                        @Valid @RequestBody ConversationMessageCreateRequest request) {
        return ApiResult.success(conversationService.sendMessage(id, request));
    }
}
