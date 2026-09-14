package com.example.smartcustomerservice.service.impl.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.SkillGroupCreateRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupQueryRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupUpdateRequest;
import com.example.smartcustomerservice.domain.entity.AssignmentRule;
import com.example.smartcustomerservice.domain.entity.SkillGroup;
import com.example.smartcustomerservice.domain.entity.SkillGroupMember;
import com.example.smartcustomerservice.domain.vo.SkillGroupVO;
import com.example.smartcustomerservice.mapper.assignment.AssignmentRuleMapper;
import com.example.smartcustomerservice.mapper.skill.SkillGroupMapper;
import com.example.smartcustomerservice.mapper.skill.SkillGroupMemberMapper;
import com.example.smartcustomerservice.service.skill.SkillGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SkillGroupServiceImpl implements SkillGroupService {

    private final SkillGroupMapper skillGroupMapper;
    private final SkillGroupMemberMapper skillGroupMemberMapper;
    private final AssignmentRuleMapper assignmentRuleMapper;

    public SkillGroupServiceImpl(SkillGroupMapper skillGroupMapper,
                                 SkillGroupMemberMapper skillGroupMemberMapper,
                                 AssignmentRuleMapper assignmentRuleMapper) {
        this.skillGroupMapper = skillGroupMapper;
        this.skillGroupMemberMapper = skillGroupMemberMapper;
        this.assignmentRuleMapper = assignmentRuleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SkillGroupVO createSkillGroup(SkillGroupCreateRequest request) {
        checkGroupNameUnique(request.getGroupName(), null);
        SkillGroup skillGroup = new SkillGroup();
        skillGroup.setGroupName(request.getGroupName());
        skillGroup.setCategory(blankToNull(request.getCategory()));
        skillGroup.setStatus(request.getStatus());
        skillGroup.setCreatedAt(LocalDateTime.now());
        if (skillGroupMapper.insert(skillGroup) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "新增技能组失败");
        }
        return toVO(skillGroup);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SkillGroupVO updateSkillGroup(Long id, SkillGroupUpdateRequest request) {
        SkillGroup skillGroup = getEntity(id);
        checkGroupNameUnique(request.getGroupName(), id);
        skillGroup.setGroupName(request.getGroupName());
        skillGroup.setCategory(blankToNull(request.getCategory()));
        skillGroup.setStatus(request.getStatus());
        if (skillGroupMapper.updateById(skillGroup) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "技能组状态已变化");
        }
        return toVO(skillGroup);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSkillGroup(Long id) {
        SkillGroup skillGroup = getEntity(id);
        if (skillGroupMemberMapper.selectCount(new LambdaQueryWrapper<SkillGroupMember>()
                .eq(SkillGroupMember::getSkillGroupId, id)) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "技能组仍存在成员，不能删除");
        }
        if (assignmentRuleMapper.selectCount(new LambdaQueryWrapper<AssignmentRule>()
                .eq(AssignmentRule::getSkillGroupId, id)) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "技能组仍被派单规则引用，不能删除");
        }
        return skillGroupMapper.deleteById(skillGroup.getId()) == 1;
    }

    @Override
    public SkillGroupVO getSkillGroup(Long id) {
        return toVO(getEntity(id));
    }

    @Override
    public PageResult<SkillGroupVO> pageSkillGroups(SkillGroupQueryRequest request) {
        SkillGroupQueryRequest safeRequest = request == null ? new SkillGroupQueryRequest() : request;
        Page<SkillGroup> page = skillGroupMapper.selectPage(
                new Page<>(safeRequest.getPage(), safeRequest.getSize()),
                new LambdaQueryWrapper<SkillGroup>()
                        .and(StringUtils.hasText(safeRequest.getKeyword()), wrapper -> wrapper
                                .like(SkillGroup::getGroupName, safeRequest.getKeyword())
                                .or()
                                .like(SkillGroup::getCategory, safeRequest.getKeyword()))
                        .eq(StringUtils.hasText(safeRequest.getCategory()), SkillGroup::getCategory, safeRequest.getCategory())
                        .eq(safeRequest.getStatus() != null, SkillGroup::getStatus, safeRequest.getStatus())
                        .orderByDesc(SkillGroup::getCreatedAt)
                        .orderByDesc(SkillGroup::getId)
        );
        List<SkillGroupVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private SkillGroup getEntity(Long id) {
        SkillGroup skillGroup = skillGroupMapper.selectById(id);
        if (skillGroup == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能组不存在");
        }
        return skillGroup;
    }

    private void checkGroupNameUnique(String groupName, Long excludeId) {
        Long count = skillGroupMapper.selectCount(new LambdaQueryWrapper<SkillGroup>()
                .eq(SkillGroup::getGroupName, groupName)
                .ne(excludeId != null, SkillGroup::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "技能组名称已存在");
        }
    }

    private SkillGroupVO toVO(SkillGroup skillGroup) {
        SkillGroupVO vo = new SkillGroupVO();
        vo.setId(skillGroup.getId());
        vo.setGroupName(skillGroup.getGroupName());
        vo.setCategory(skillGroup.getCategory());
        vo.setStatus(skillGroup.getStatus());
        vo.setCreatedAt(skillGroup.getCreatedAt());
        return vo;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
