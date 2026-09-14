package com.example.smartcustomerservice.service.assignment;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleCreateRequest;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleQueryRequest;
import com.example.smartcustomerservice.domain.dto.AssignmentRuleUpdateRequest;
import com.example.smartcustomerservice.domain.vo.AssignmentRuleVO;

// AssignmentRuleService 属于智能客服平台基础代码。
public interface AssignmentRuleService {

    AssignmentRuleVO createRule(AssignmentRuleCreateRequest request);

    AssignmentRuleVO updateRule(AssignmentRuleUpdateRequest request);

    Boolean deleteRule(Long id);

    AssignmentRuleVO getRule(Long id);

    PageResult<AssignmentRuleVO> pageRules(AssignmentRuleQueryRequest request);
}
