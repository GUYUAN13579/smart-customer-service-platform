package com.example.smartcustomerservice.service.impl.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberCreateRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberQueryRequest;
import com.example.smartcustomerservice.domain.dto.SkillGroupMemberUpdateRequest;
import com.example.smartcustomerservice.domain.entity.SkillGroup;
import com.example.smartcustomerservice.domain.entity.SkillGroupMember;
import com.example.smartcustomerservice.domain.entity.SysRole;
import com.example.smartcustomerservice.domain.entity.SysUser;
import com.example.smartcustomerservice.domain.entity.SysUserRole;
import com.example.smartcustomerservice.domain.vo.SkillGroupMemberVO;
import com.example.smartcustomerservice.mapper.auth.SysRoleMapper;
import com.example.smartcustomerservice.mapper.auth.SysUserMapper;
import com.example.smartcustomerservice.mapper.auth.SysUserRoleMapper;
import com.example.smartcustomerservice.mapper.skill.SkillGroupMapper;
import com.example.smartcustomerservice.mapper.skill.SkillGroupMemberMapper;
import com.example.smartcustomerservice.service.skill.SkillGroupMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SkillGroupMemberServiceImpl implements SkillGroupMemberService {

    private final SkillGroupMapper skillGroupMapper;
    private final SkillGroupMemberMapper skillGroupMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public SkillGroupMemberServiceImpl(SkillGroupMapper skillGroupMapper,
                                       SkillGroupMemberMapper skillGroupMemberMapper,
                                       SysUserMapper sysUserMapper,
                                       SysRoleMapper sysRoleMapper,
                                       SysUserRoleMapper sysUserRoleMapper) {
        this.skillGroupMapper = skillGroupMapper;
        this.skillGroupMemberMapper = skillGroupMemberMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SkillGroupMemberVO createMember(Long skillGroupId, SkillGroupMemberCreateRequest request) {
        SkillGroup skillGroup = getSkillGroup(skillGroupId);
        if (!Integer.valueOf(1).equals(skillGroup.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "技能组已停用，不能新增成员");
        }
        SysUser user = getEnabledAgent(request.getUserId());

        Long count = skillGroupMemberMapper.selectCount(new LambdaQueryWrapper<SkillGroupMember>()
                .eq(SkillGroupMember::getSkillGroupId, skillGroupId)
                .eq(SkillGroupMember::getUserId, request.getUserId()));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该客服已在技能组中");
        }

        SkillGroupMember member = new SkillGroupMember();
        member.setSkillGroupId(skillGroupId);
        member.setUserId(request.getUserId());
        member.setMaxActiveTickets(request.getMaxActiveTickets());
        member.setStatus(request.getStatus());
        member.setCreatedAt(LocalDateTime.now());
        if (skillGroupMemberMapper.insert(member) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "新增技能组成员失败");
        }
        return toVO(member, skillGroup, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SkillGroupMemberVO updateMember(Long skillGroupId, Long memberId, SkillGroupMemberUpdateRequest request) {
        if (request.getMaxActiveTickets() == null && request.getStatus() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少修改一项成员配置");
        }
        SkillGroup skillGroup = getSkillGroup(skillGroupId);
        SkillGroupMember member = getMemberEntity(skillGroupId, memberId);
        if (request.getMaxActiveTickets() != null) {
            member.setMaxActiveTickets(request.getMaxActiveTickets());
        }
        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());
        }
        if (skillGroupMemberMapper.updateById(member) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "技能组成员状态已变化");
        }
        return toVO(member, skillGroup, getUser(member.getUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteMember(Long skillGroupId, Long memberId) {
        getSkillGroup(skillGroupId);
        SkillGroupMember member = getMemberEntity(skillGroupId, memberId);
        return skillGroupMemberMapper.deleteById(member.getId()) == 1;
    }

    @Override
    public SkillGroupMemberVO getMember(Long skillGroupId, Long memberId) {
        SkillGroup skillGroup = getSkillGroup(skillGroupId);
        SkillGroupMember member = getMemberEntity(skillGroupId, memberId);
        return toVO(member, skillGroup, getUser(member.getUserId()));
    }

    @Override
    public PageResult<SkillGroupMemberVO> pageMembers(Long skillGroupId, SkillGroupMemberQueryRequest request) {
        SkillGroup skillGroup = getSkillGroup(skillGroupId);
        SkillGroupMemberQueryRequest safeRequest = request == null ? new SkillGroupMemberQueryRequest() : request;
        Page<SkillGroupMember> page = skillGroupMemberMapper.selectPage(
                new Page<>(safeRequest.getPage(), safeRequest.getSize()),
                new LambdaQueryWrapper<SkillGroupMember>()
                        .eq(SkillGroupMember::getSkillGroupId, skillGroupId)
                        .eq(safeRequest.getUserId() != null, SkillGroupMember::getUserId, safeRequest.getUserId())
                        .eq(safeRequest.getStatus() != null, SkillGroupMember::getStatus, safeRequest.getStatus())
                        .orderByDesc(SkillGroupMember::getCreatedAt)
                        .orderByDesc(SkillGroupMember::getId)
        );
        List<SkillGroupMemberVO> records = page.getRecords().stream()
                .map(member -> toVO(member, skillGroup, getUser(member.getUserId())))
                .toList();
        return PageResult.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private SkillGroup getSkillGroup(Long skillGroupId) {
        SkillGroup skillGroup = skillGroupMapper.selectById(skillGroupId);
        if (skillGroup == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能组不存在");
        }
        return skillGroup;
    }

    private SkillGroupMember getMemberEntity(Long skillGroupId, Long memberId) {
        SkillGroupMember member = skillGroupMemberMapper.selectOne(new LambdaQueryWrapper<SkillGroupMember>()
                .eq(SkillGroupMember::getId, memberId)
                .eq(SkillGroupMember::getSkillGroupId, skillGroupId));
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能组成员不存在");
        }
        return member;
    }

    private SysUser getEnabledAgent(Long userId) {
        SysUser user = getUser(userId);
        if (!Integer.valueOf(1).equals(user.getStatus()) || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.CONFLICT, "客服账号已停用");
        }

        SysRole agentRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "AGENT")
                .eq(SysRole::getStatus, 1)
                .last("LIMIT 1"));
        if (agentRole == null || sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, agentRole.getId())) == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能将 AGENT 角色用户加入技能组");
        }
        return user;
    }

    private SysUser getUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private SkillGroupMemberVO toVO(SkillGroupMember member, SkillGroup skillGroup, SysUser user) {
        SkillGroupMemberVO vo = new SkillGroupMemberVO();
        vo.setId(member.getId());
        vo.setSkillGroupId(member.getSkillGroupId());
        vo.setSkillGroupName(skillGroup.getGroupName());
        vo.setSkillGroupCategory(skillGroup.getCategory());
        vo.setUserId(member.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setMaxActiveTickets(member.getMaxActiveTickets());
        vo.setStatus(member.getStatus());
        vo.setCreatedAt(member.getCreatedAt());
        return vo;
    }
}
