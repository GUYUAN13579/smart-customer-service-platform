package com.example.smartcustomerservice.controller.knowledge;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeIndexTaskQueryRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeIndexTaskVO;
import com.example.smartcustomerservice.service.knowledge.KnowledgeIndexTaskService;
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

@Tag(name = "知识索引任务管理")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/knowledge")
public class KnowledgeIndexTaskController {

    private final KnowledgeIndexTaskService knowledgeIndexTaskService;

    public KnowledgeIndexTaskController(KnowledgeIndexTaskService knowledgeIndexTaskService) {
        this.knowledgeIndexTaskService = knowledgeIndexTaskService;
    }

    @Operation(summary = "创建知识文章索引任务")
    @PostMapping("/articles/{id}/index")
    @PreAuthorize("hasAuthority('knowledge:index:create')")
    public ApiResult<KnowledgeIndexTaskVO> createArticleIndexTask(
            @NotNull(message = "文章ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeIndexTaskService.createArticleIndexTask(id));
    }

    @Operation(summary = "创建知识库文件索引任务")
    @PostMapping("/documents/{id}/index")
    @PreAuthorize("hasAuthority('knowledge:index:create')")
    public ApiResult<KnowledgeIndexTaskVO> createDocumentIndexTask(
            @NotNull(message = "文档ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeIndexTaskService.createDocumentIndexTask(id));
    }

    @Operation(summary = "分页查询知识索引任务")
    @GetMapping("/index-tasks")
    @PreAuthorize("hasAuthority('knowledge:index:list')")
    public ApiResult<PageResult<KnowledgeIndexTaskVO>> pageIndexTasks(
            @Valid KnowledgeIndexTaskQueryRequest request) {
        return ApiResult.success(knowledgeIndexTaskService.pageIndexTasks(request));
    }

    @Operation(summary = "查询知识索引任务详情")
    @GetMapping("/index-tasks/{id}")
    @PreAuthorize("hasAuthority('knowledge:index:detail')")
    public ApiResult<KnowledgeIndexTaskVO> getIndexTask(
            @NotNull(message = "索引任务ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeIndexTaskService.getIndexTask(id));
    }

    @Operation(summary = "重试失败的知识索引任务")
    @PostMapping("/index-tasks/{id}/retry")
    @PreAuthorize("hasAuthority('knowledge:index:retry')")
    public ApiResult<KnowledgeIndexTaskVO> retryIndexTask(
            @NotNull(message = "索引任务ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeIndexTaskService.retryIndexTask(id));
    }

    @Operation(summary = "执行待处理的知识索引任务")
    @PostMapping("/index-tasks/{id}/execute")
    @PreAuthorize("hasAuthority('knowledge:index:execute')")
    public ApiResult<KnowledgeIndexTaskVO> executeIndexTask(
            @NotNull(message = "索引任务ID不能为空") @PathVariable Long id) {
        return ApiResult.success(knowledgeIndexTaskService.executeIndexTask(id));
    }
}
