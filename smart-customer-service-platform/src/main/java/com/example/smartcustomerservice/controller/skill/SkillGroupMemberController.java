package com.example.smartcustomerservice.controller.skill;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberCreateRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberQueryRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberUpdateRequest;
import com.example.smartcustomerservice.domain.vo.SkillGroupMemberVO;
import com.example.smartcustomerservice.service.skill.SkillGroupMemberService;
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

@Tag(name = "技能组成员管理")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/skill-groups/{skillGroupId}/members")
public class SkillGroupMemberController {

    private final SkillGroupMemberService skillGroupMemberService;

    public SkillGroupMemberController(SkillGroupMemberService skillGroupMemberService) {
        this.skillGroupMemberService = skillGroupMemberService;
    }

    @Operation(summary = "新增技能组成员")
    @PostMapping
    @PreAuthorize("hasAuthority('skill-group-member:create')")
    public ApiResult<SkillGroupMemberVO> createMember(@NotNull(message = "技能组ID不能为空") @PathVariable Long skillGroupId,
                                                       @Valid @RequestBody SkillGroupMemberCreateRequest request) {
        return ApiResult.success(skillGroupMemberService.createMember(skillGroupId, request));
    }

    @Operation(summary = "修改技能组成员配置")
    @PutMapping("/{memberId}")
    @PreAuthorize("hasAuthority('skill-group-member:update')")
    public ApiResult<SkillGroupMemberVO> updateMember(@NotNull(message = "技能组ID不能为空") @PathVariable Long skillGroupId,
                                                       @NotNull(message = "成员ID不能为空") @PathVariable Long memberId,
                                                       @Valid @RequestBody SkillGroupMemberUpdateRequest request) {
        return ApiResult.success(skillGroupMemberService.updateMember(skillGroupId, memberId, request));
    }

    @Operation(summary = "移除技能组成员")
    @DeleteMapping("/{memberId}")
    @PreAuthorize("hasAuthority('skill-group-member:delete')")
    public ApiResult<Boolean> deleteMember(@NotNull(message = "技能组ID不能为空") @PathVariable Long skillGroupId,
                                           @NotNull(message = "成员ID不能为空") @PathVariable Long memberId) {
        return ApiResult.success(skillGroupMemberService.deleteMember(skillGroupId, memberId));
    }

    @Operation(summary = "查询技能组成员详情")
    @GetMapping("/{memberId}")
    @PreAuthorize("hasAuthority('skill-group-member:detail')")
    public ApiResult<SkillGroupMemberVO> getMember(@NotNull(message = "技能组ID不能为空") @PathVariable Long skillGroupId,
                                                   @NotNull(message = "成员ID不能为空") @PathVariable Long memberId) {
        return ApiResult.success(skillGroupMemberService.getMember(skillGroupId, memberId));
    }

    @Operation(summary = "分页查询技能组成员")
    @GetMapping
    @PreAuthorize("hasAuthority('skill-group-member:list')")
    public ApiResult<PageResult<SkillGroupMemberVO>> pageMembers(@NotNull(message = "技能组ID不能为空") @PathVariable Long skillGroupId,
                                                                  @Valid SkillGroupMemberQueryRequest request) {
        return ApiResult.success(skillGroupMemberService.pageMembers(skillGroupId, request));
    }
}
