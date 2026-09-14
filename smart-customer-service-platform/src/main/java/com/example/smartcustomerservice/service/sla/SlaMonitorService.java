package com.example.smartcustomerservice.service.sla;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.SlaTicketRemindRequest;
import com.example.smartcustomerservice.domain.dto.SlaTicketMonitorQueryRequest;
import com.example.smartcustomerservice.domain.vo.SlaTicketRemindVO;
import com.example.smartcustomerservice.domain.vo.SlaTicketMonitorVO;

// SlaMonitorService 属于智能客服平台基础代码。
public interface SlaMonitorService {

    PageResult<SlaTicketMonitorVO> pageRiskTickets(SlaTicketMonitorQueryRequest request);

    PageResult<SlaTicketMonitorVO> pageOverdueTickets(SlaTicketMonitorQueryRequest request);

    SlaTicketRemindVO remindTicket(Long ticketId, SlaTicketRemindRequest request);
}
