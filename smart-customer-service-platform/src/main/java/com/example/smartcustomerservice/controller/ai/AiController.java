package com.example.smartcustomerservice.controller.ai;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.dto.AiAutoReplyRequest;
import com.example.smartcustomerservice.domain.dto.AiChatRequest;
import com.example.smartcustomerservice.domain.dto.AiSessionSummaryRequest;
import com.example.smartcustomerservice.domain.dto.AiTicketDraftRequest;
import com.example.smartcustomerservice.domain.vo.AiAutoReplyVO;
import com.example.smartcustomerservice.domain.vo.AiChatVO;
import com.example.smartcustomerservice.domain.vo.AiSessionSummaryVO;
import com.example.smartcustomerservice.domain.vo.AiTicketDraftVO;
import com.example.smartcustomerservice.service.ai.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI服务")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/ai")
// AiController 属于智能客服平台基础代码。
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "AI 问答")
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResult<AiChatVO> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResult.success(aiService.chat(request));
    }

    @Operation(summary = "AI 会话总结")
    @PostMapping("/conversations/{sessionId}/summary")
    @PreAuthorize("hasAuthority('ai:summary')")
    public ApiResult<AiSessionSummaryVO> summarizeSession(
            @NotNull(message = "会话ID不能为空") @PathVariable Long sessionId,
            @Valid @RequestBody(required = false) AiSessionSummaryRequest request) {
        return ApiResult.success(aiService.summarizeSession(sessionId, request));
    }

    @Operation(summary = "AI 自动回复")
    @PostMapping("/conversations/{sessionId}/auto-reply")
    @PreAuthorize("hasAuthority('ai:auto-reply')")
    public ApiResult<AiAutoReplyVO> autoReply(
            @NotNull(message = "会话ID不能为空") @PathVariable Long sessionId,
            @Valid @RequestBody(required = false) AiAutoReplyRequest request) {
        return ApiResult.success(aiService.autoReply(sessionId, request));
    }

    @Operation(summary = "AI 生成工单草稿")
    @PostMapping("/conversations/{sessionId}/ticket-draft")
    @PreAuthorize("hasAuthority('ai:ticket-draft')")
    public ApiResult<AiTicketDraftVO> generateTicketDraft(
            @NotNull(message = "会话ID不能为空") @PathVariable Long sessionId,
            @Valid @RequestBody(required = false) AiTicketDraftRequest request) {
        return ApiResult.success(aiService.generateTicketDraft(sessionId, request));
    }
}
