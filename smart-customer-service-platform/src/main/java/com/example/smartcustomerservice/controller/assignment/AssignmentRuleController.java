package com.example.smartcustomerservice.controller.assignment;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleCreateRequest;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleQueryRequest;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleUpdateRequest;
import com.example.smartcustomerservice.domain.vo.AssignmentRuleVO;
import com.example.smartcustomerservice.service.assignment.AssignmentRuleService;
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

@Tag(name = "派单规则")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/assignment-rules")
public class AssignmentRuleController {

    private final AssignmentRuleService assignmentRuleService;

    public AssignmentRuleController(AssignmentRuleService assignmentRuleService) {
        this.assignmentRuleService = assignmentRuleService;
    }

    @Operation(summary = "创建派单规则")
    @PostMapping
    @PreAuthorize("hasAuthority('assignment-rule:create')")
    public ApiResult<AssignmentRuleVO> createRule(@Valid @RequestBody AssignmentRuleCreateRequest request) {
        return ApiResult.success(assignmentRuleService.createRule(request));
    }

    @Operation(summary = "修改派单规则")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('assignment-rule:update')")
    public ApiResult<AssignmentRuleVO> updateRule(@NotNull(message = "规则ID不能为空") @PathVariable Long id,
                                                  @Valid @RequestBody AssignmentRuleUpdateRequest request) {
        request.setId(id);
        return ApiResult.success(assignmentRuleService.updateRule(request));
    }

    @Operation(summary = "删除派单规则")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('assignment-rule:delete')")
    public ApiResult<Boolean> deleteRule(@NotNull(message = "规则ID不能为空") @PathVariable Long id) {
        return ApiResult.success(assignmentRuleService.deleteRule(id));
    }

    @Operation(summary = "派单规则详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('assignment-rule:detail')")
    public ApiResult<AssignmentRuleVO> getRule(@NotNull(message = "规则ID不能为空") @PathVariable Long id) {
        return ApiResult.success(assignmentRuleService.getRule(id));
    }

    @Operation(summary = "派单规则分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('assignment-rule:list')")
    public ApiResult<PageResult<AssignmentRuleVO>> pageRules(@Valid AssignmentRuleQueryRequest request) {
        return ApiResult.success(assignmentRuleService.pageRules(request));
    }
}
