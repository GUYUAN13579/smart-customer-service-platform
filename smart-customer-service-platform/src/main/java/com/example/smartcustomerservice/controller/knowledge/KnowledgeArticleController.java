package com.example.smartcustomerservice.controller.knowledge;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleQueryRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleUpdateRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeArticleVO;
import com.example.smartcustomerservice.service.knowledge.KnowledgeArticleService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识文章管理")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/knowledge/articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    public KnowledgeArticleController(KnowledgeArticleService knowledgeArticleService) {
        this.knowledgeArticleService = knowledgeArticleService;
    }

    @Operation(summary = "新增知识文章")
    @PostMapping
    @PreAuthorize("hasAuthority('knowledge:article:create')")
    public ApiResult<KnowledgeArticleVO> createArticle(@Valid @RequestBody KnowledgeArticleCreateRequest request) {
        return ApiResult.success(knowledgeArticleService.createArticle(request));
    }

    @Operation(summary = "修改知识文章")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('knowledge:article:update')")
    public ApiResult<KnowledgeArticleVO> updateArticle(@NotNull(message = "文章ID不能为空") @PathVariable Long id,
                                                        @Valid @RequestBody KnowledgeArticleUpdateRequest request) {
        return ApiResult.success(knowledgeArticleService.updateArticle(id, request));
    }

    @Operation(summary = "删除知识文章")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('knowledge:article:delete')")
    public ApiResult<Boolean> deleteArticle(@NotNull(message = "文章ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeArticleService.deleteArticle(id));
    }

    @Operation(summary = "查询知识文章详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('knowledge:article:detail')")
    public ApiResult<KnowledgeArticleVO> getArticle(@NotNull(message = "文章ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeArticleService.getArticle(id));
    }

    @Operation(summary = "分页查询知识文章")
    @GetMapping
    @PreAuthorize("hasAuthority('knowledge:article:list')")
    public ApiResult<PageResult<KnowledgeArticleVO>> pageArticles(@Valid KnowledgeArticleQueryRequest request) {
        return ApiResult.success(knowledgeArticleService.pageArticles(request));
    }

    @Operation(summary = "发布知识文章")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('knowledge:article:publish')")
    public ApiResult<KnowledgeArticleVO> publishArticle(@NotNull(message = "文章ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeArticleService.publishArticle(id));
    }

    @Operation(summary = "下线知识文章")
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasAuthority('knowledge:article:offline')")
    public ApiResult<KnowledgeArticleVO> offlineArticle(@NotNull(message = "文章ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeArticleService.offlineArticle(id));
    }
}
