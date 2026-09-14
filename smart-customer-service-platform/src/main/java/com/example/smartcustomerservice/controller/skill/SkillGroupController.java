package com.example.smartcustomerservice.controller.skill;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SkillGroupCreateRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupQueryRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupUpdateRequest;
import com.example.smartcustomerservice.domain.vo.SkillGroupVO;
import com.example.smartcustomerservice.service.skill.SkillGroupService;
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

@Tag(name = "技能组管理")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/skill-groups")
public class SkillGroupController {

    private final SkillGroupService skillGroupService;

    public SkillGroupController(SkillGroupService skillGroupService) {
        this.skillGroupService = skillGroupService;
    }

    @Operation(summary = "新增技能组")
    @PostMapping
    @PreAuthorize("hasAuthority('skill-group:create')")
    public ApiResult<SkillGroupVO> createSkillGroup(@Valid @RequestBody SkillGroupCreateRequest request) {
        return ApiResult.success(skillGroupService.createSkillGroup(request));
    }

    @Operation(summary = "修改技能组")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('skill-group:update')")
    public ApiResult<SkillGroupVO> updateSkillGroup(@NotNull(message = "技能组ID不能为空") @PathVariable Long id,
                                                     @Valid @RequestBody SkillGroupUpdateRequest request) {
        return ApiResult.success(skillGroupService.updateSkillGroup(id, request));
    }

    @Operation(summary = "删除技能组")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('skill-group:delete')")
    public ApiResult<Boolean> deleteSkillGroup(@NotNull(message = "技能组ID不能为空") @PathVariable Long id) {
        return ApiResult.success(skillGroupService.deleteSkillGroup(id));
    }

    @Operation(summary = "查询技能组详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('skill-group:detail')")
    public ApiResult<SkillGroupVO> getSkillGroup(@NotNull(message = "技能组ID不能为空") @PathVariable Long id) {
        return ApiResult.success(skillGroupService.getSkillGroup(id));
    }

    @Operation(summary = "分页查询技能组")
    @GetMapping
    @PreAuthorize("hasAuthority('skill-group:list')")
    public ApiResult<PageResult<SkillGroupVO>> pageSkillGroups(@Valid SkillGroupQueryRequest request) {
        return ApiResult.success(skillGroupService.pageSkillGroups(request));
    }
}
