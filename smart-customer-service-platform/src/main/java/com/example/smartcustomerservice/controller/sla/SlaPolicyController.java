package com.example.smartcustomerservice.controller.sla;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SlaPolicyCreateRequest;
import com.example.smartcustomerservice.domain.dto.SlaPolicyQueryRequest;
import com.example.smartcustomerservice.domain.dto.SlaPolicyUpdateRequest;
import com.example.smartcustomerservice.domain.vo.SlaPolicyVO;
import com.example.smartcustomerservice.service.sla.SlaPolicyService;
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

@Tag(name = "SLA策略")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/sla/policies")
// SlaPolicyController 属于智能客服平台基础代码。
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;

    public SlaPolicyController(SlaPolicyService slaPolicyService) {
        this.slaPolicyService = slaPolicyService;
    }

    @Operation(summary = "创建SLA策略")
    @PostMapping
    @PreAuthorize("hasAuthority('sla-policy:create')")
    public ApiResult<SlaPolicyVO> createPolicy(@Valid @RequestBody SlaPolicyCreateRequest request) {
        return ApiResult.success(slaPolicyService.createPolicy(request));
    }

    @Operation(summary = "修改SLA策略")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sla-policy:update')")
    public ApiResult<SlaPolicyVO> updatePolicy(@NotNull(message = "SLA策略ID不能为空") @PathVariable Long id,
                                               @Valid @RequestBody SlaPolicyUpdateRequest request) {
        request.setId(id);
        return ApiResult.success(slaPolicyService.updatePolicy(request));
    }

    @Operation(summary = "删除SLA策略")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sla-policy:delete')")
    public ApiResult<Boolean> deletePolicy(@NotNull(message = "SLA策略ID不能为空") @PathVariable Long id) {
        return ApiResult.success(slaPolicyService.deletePolicy(id));
    }

    @Operation(summary = "SLA策略详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sla-policy:detail')")
    public ApiResult<SlaPolicyVO> getPolicy(@NotNull(message = "SLA策略ID不能为空") @PathVariable Long id) {
        return ApiResult.success(slaPolicyService.getPolicy(id));
    }

    @Operation(summary = "SLA策略分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('sla-policy:list')")
    public ApiResult<PageResult<SlaPolicyVO>> pagePolicies(@Valid SlaPolicyQueryRequest request) {
        return ApiResult.success(slaPolicyService.pagePolicies(request));
    }
}
