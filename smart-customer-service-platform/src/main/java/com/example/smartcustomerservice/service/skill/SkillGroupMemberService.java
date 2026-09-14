package com.example.smartcustomerservice.service.skill;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberCreateRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberQueryRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberUpdateRequest;
import com.example.smartcustomerservice.domain.vo.SkillGroupMemberVO;

public interface SkillGroupMemberService {

    SkillGroupMemberVO createMember(Long skillGroupId, SkillGroupMemberCreateRequest request);

    SkillGroupMemberVO updateMember(Long skillGroupId, Long memberId, SkillGroupMemberUpdateRequest request);

    Boolean deleteMember(Long skillGroupId, Long memberId);

    SkillGroupMemberVO getMember(Long skillGroupId, Long memberId);

    PageResult<SkillGroupMemberVO> pageMembers(Long skillGroupId, SkillGroupMemberQueryRequest request);
}
