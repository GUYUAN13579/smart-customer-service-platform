package com.example.smartcustomerservice.service.sla;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SlaAlertQueryRequest;
import com.example.smartcustomerservice.domain.vo.SlaAlertVO;

// SlaAlertService 属于智能客服平台基础代码。
public interface SlaAlertService {

    PageResult<SlaAlertVO> pageAlerts(SlaAlertQueryRequest request);

    SlaAlertVO getAlert(Long id);

    SlaAlertVO retryAlert(Long id);
}
