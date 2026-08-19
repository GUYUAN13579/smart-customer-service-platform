package com.example.smartcustomerservice.service.impl.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleCreateRequest;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleQueryRequest;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleUpdateRequest;
import com.example.smartcustomerservice.domain.entity.AssignmentRule;
import com.example.smartcustomerservice.domain.vo.AssignmentRuleVO;
import com.example.smartcustomerservice.mapper.assignment.AssignmentRuleMapper;
import com.example.smartcustomerservice.service.assignment.AssignmentRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentRuleServiceImpl implements AssignmentRuleService {

    private final AssignmentRuleMapper assignmentRuleMapper;

    public AssignmentRuleServiceImpl(AssignmentRuleMapper assignmentRuleMapper) {
        this.assignmentRuleMapper = assignmentRuleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentRuleVO createRule(AssignmentRuleCreateRequest request) {
        AssignmentRule rule = new AssignmentRule();
        rule.setRuleName(request.getRuleName());
        rule.setCategory(blankToNull(request.getCategory()));
        rule.setPriority(blankToNull(request.getPriority()));
        rule.setSkillGroupId(request.getSkillGroupId());
        rule.setRuleWeight(defaultZero(request.getRuleWeight()));
        rule.setEnabled(defaultOne(request.getEnabled()));
        rule.setCreatedAt(LocalDateTime.now());
        assignmentRuleMapper.insert(rule);
        return toVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentRuleVO updateRule(AssignmentRuleUpdateRequest request) {
        AssignmentRule existing = assignmentRuleMapper.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "派单规则不存在");
        }

        existing.setRuleName(request.getRuleName());
        existing.setCategory(blankToNull(request.getCategory()));
        existing.setPriority(blankToNull(request.getPriority()));
        existing.setSkillGroupId(request.getSkillGroupId());
        existing.setRuleWeight(defaultZero(request.getRuleWeight()));
        existing.setEnabled(defaultOne(request.getEnabled()));
        assignmentRuleMapper.updateById(existing);
        return toVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRule(Long id) {
        AssignmentRule existing = assignmentRuleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "派单规则不存在");
        }
        return assignmentRuleMapper.deleteById(id) > 0;
    }

    @Override
    public AssignmentRuleVO getRule(Long id) {
        AssignmentRule rule = assignmentRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "派单规则不存在");
        }
        return toVO(rule);
    }

    @Override
    public PageResult<AssignmentRuleVO> pageRules(AssignmentRuleQueryRequest request) {
        LambdaQueryWrapper<AssignmentRule> wrapper = new LambdaQueryWrapper<AssignmentRule>()
                .like(StringUtils.hasText(request.getKeyword()), AssignmentRule::getRuleName, request.getKeyword())
                .eq(StringUtils.hasText(request.getCategory()), AssignmentRule::getCategory, request.getCategory())
                .eq(StringUtils.hasText(request.getPriority()), AssignmentRule::getPriority, request.getPriority())
                .eq(request.getEnabled() != null, AssignmentRule::getEnabled, request.getEnabled())
                .orderByDesc(AssignmentRule::getRuleWeight)
                .orderByDesc(AssignmentRule::getId);

        Page<AssignmentRule> page = assignmentRuleMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
        List<AssignmentRuleVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private AssignmentRuleVO toVO(AssignmentRule rule) {
        AssignmentRuleVO vo = new AssignmentRuleVO();
        vo.setId(rule.getId());
        vo.setRuleName(rule.getRuleName());
        vo.setCategory(rule.getCategory());
        vo.setPriority(rule.getPriority());
        vo.setSkillGroupId(rule.getSkillGroupId());
        vo.setRuleWeight(rule.getRuleWeight());
        vo.setEnabled(rule.getEnabled());
        vo.setCreatedAt(rule.getCreatedAt());
        return vo;
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer defaultOne(Integer value) {
        return value == null ? 1 : value;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
