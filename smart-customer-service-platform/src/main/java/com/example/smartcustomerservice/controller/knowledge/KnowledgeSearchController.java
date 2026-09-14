package com.example.smartcustomerservice.controller.knowledge;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeSearchRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeSearchVO;
import com.example.smartcustomerservice.service.knowledge.KnowledgeSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "知识语义检索")
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/knowledge")
public class KnowledgeSearchController {

    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeSearchController(KnowledgeSearchService knowledgeSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @Operation(summary = "语义检索知识切片")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('knowledge:search')")
    public ApiResult<List<KnowledgeSearchVO>> search(@Valid KnowledgeSearchRequest request) {
        return ApiResult.success(knowledgeSearchService.search(request));
    }
}
