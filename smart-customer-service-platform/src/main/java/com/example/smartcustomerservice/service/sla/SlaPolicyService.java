package com.example.smartcustomerservice.service.sla;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SlaPolicyCreateRequest;
import com.example.smartcustomerservice.domain.dto.SlaPolicyQueryRequest;
import com.example.smartcustomerservice.domain.dto.SlaPolicyUpdateRequest;
import com.example.smartcustomerservice.domain.vo.SlaPolicyVO;

// SlaPolicyService 属于智能客服平台基础代码。
public interface SlaPolicyService {

    SlaPolicyVO createPolicy(SlaPolicyCreateRequest request);

    SlaPolicyVO updatePolicy(SlaPolicyUpdateRequest request);

    Boolean deletePolicy(Long id);

    SlaPolicyVO getPolicy(Long id);

    PageResult<SlaPolicyVO> pagePolicies(SlaPolicyQueryRequest request);
}
