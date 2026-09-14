package com.example.smartcustomerservice.controller.knowledge;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeChunkCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeChunkQueryRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeChunkRebuildVO;
import com.example.smartcustomerservice.domain.vo.KnowledgeChunkVO;
import com.example.smartcustomerservice.service.knowledge.KnowledgeChunkService;
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

@Tag(name = "知识切片管理")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/knowledge")
public class KnowledgeChunkController {

    private final KnowledgeChunkService knowledgeChunkService;

    public KnowledgeChunkController(KnowledgeChunkService knowledgeChunkService) {
        this.knowledgeChunkService = knowledgeChunkService;
    }

    @Operation(summary = "新增单条知识切片")
    @PostMapping("/chunks")
    @PreAuthorize("hasAuthority('knowledge:chunk:create')")
    public ApiResult<KnowledgeChunkVO> createChunk(@Valid @RequestBody KnowledgeChunkCreateRequest request) {
        return ApiResult.success(knowledgeChunkService.createChunk(request));
    }

    @Operation(summary = "重建知识文章切片")
    @PostMapping("/articles/{id}/chunks/rebuild")
    @PreAuthorize("hasAuthority('knowledge:chunk:rebuild')")
    public ApiResult<KnowledgeChunkRebuildVO> rebuildArticleChunks(
            @NotNull(message = "文章ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeChunkService.rebuildArticleChunks(id));
    }

    @Operation(summary = "重建知识库文件切片")
    @PostMapping("/documents/{id}/chunks/rebuild")
    @PreAuthorize("hasAuthority('knowledge:chunk:rebuild')")
    public ApiResult<KnowledgeChunkRebuildVO> rebuildDocumentChunks(
            @NotNull(message = "文档ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeChunkService.rebuildDocumentChunks(id));
    }

    @Operation(summary = "分页查询知识切片")
    @GetMapping("/chunks")
    @PreAuthorize("hasAuthority('knowledge:chunk:list')")
    public ApiResult<PageResult<KnowledgeChunkVO>> pageChunks(@Valid KnowledgeChunkQueryRequest request) {
        return ApiResult.success(knowledgeChunkService.pageChunks(request));
    }

    @Operation(summary = "查询知识切片详情")
    @GetMapping("/chunks/{id}")
    @PreAuthorize("hasAuthority('knowledge:chunk:detail')")
    public ApiResult<KnowledgeChunkVO> getChunk(@NotNull(message = "切片ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeChunkService.getChunk(id));
    }
}
