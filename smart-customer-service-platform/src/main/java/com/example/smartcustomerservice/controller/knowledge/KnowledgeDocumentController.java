package com.example.smartcustomerservice.controller.knowledge;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeDocumentCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeDocumentQueryRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeDocumentVO;
import com.example.smartcustomerservice.service.knowledge.KnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库文件管理")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/knowledge/documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    public KnowledgeDocumentController(KnowledgeDocumentService knowledgeDocumentService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    @Operation(summary = "导入已上传的纯文本文件")
    @PostMapping
    @PreAuthorize("hasAuthority('knowledge:document:create')")
    public ApiResult<KnowledgeDocumentVO> createDocument(@Valid @RequestBody KnowledgeDocumentCreateRequest request) {
        return ApiResult.success(knowledgeDocumentService.createDocument(request));
    }

    @Operation(summary = "分页查询知识库导入文件")
    @GetMapping
    @PreAuthorize("hasAuthority('knowledge:document:list')")
    public ApiResult<PageResult<KnowledgeDocumentVO>> pageDocuments(@Valid KnowledgeDocumentQueryRequest request) {
        return ApiResult.success(knowledgeDocumentService.pageDocuments(request));
    }

    @Operation(summary = "查询知识库导入文件详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('knowledge:document:detail')")
    public ApiResult<KnowledgeDocumentVO> getDocument(@NotNull(message = "文档ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeDocumentService.getDocument(id));
    }

    @Operation(summary = "解析纯文本知识库文件")
    @PostMapping("/{id}/parse")
    @PreAuthorize("hasAuthority('knowledge:document:parse')")
    public ApiResult<KnowledgeDocumentVO> parseDocument(@NotNull(message = "文档ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeDocumentService.parseDocument(id));
    }

    @Operation(summary = "重试解析纯文本知识库文件")
    @PostMapping("/{id}/retry-parse")
    @PreAuthorize("hasAuthority('knowledge:document:retry')")
    public ApiResult<KnowledgeDocumentVO> retryParseDocument(@NotNull(message = "文档ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeDocumentService.retryParseDocument(id));
    }

    @Operation(summary = "下线知识库导入文件")
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasAuthority('knowledge:document:offline')")
    public ApiResult<KnowledgeDocumentVO> offlineDocument(@NotNull(message = "文档ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeDocumentService.offlineDocument(id));
    }

    @Operation(summary = "删除知识库导入文件")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('knowledge:document:delete')")
    public ApiResult<Boolean> deleteDocument(@NotNull(message = "文档ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeDocumentService.deleteDocument(id));
    }
}
