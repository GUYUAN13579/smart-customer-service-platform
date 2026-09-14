package com.example.smartcustomerservice.service.skill;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SkillGroupCreateRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupQueryRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupUpdateRequest;
import com.example.smartcustomerservice.domain.vo.SkillGroupVO;

public interface SkillGroupService {

    SkillGroupVO createSkillGroup(SkillGroupCreateRequest request);

    SkillGroupVO updateSkillGroup(Long id, SkillGroupUpdateRequest request);

    Boolean deleteSkillGroup(Long id);

    SkillGroupVO getSkillGroup(Long id);

    PageResult<SkillGroupVO> pageSkillGroups(SkillGroupQueryRequest request);
}
