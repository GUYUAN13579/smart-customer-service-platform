package com.example.smartcustomerservice.service.ticket;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.ConversationManualTransferRequest;
import com.example.smartcustomerservice.domain.dto.TicketApplySlaRequest;
import com.example.smartcustomerservice.domain.dto.TicketAssignRequest;
import com.example.smartcustomerservice.domain.dto.TicketCloseRequest;
import com.example.smartcustomerservice.domain.dto.TicketOperationLogQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketProcessRecordCreateRequest;
import com.example.smartcustomerservice.domain.dto.TicketProcessRecordQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketReviewRequest;
import com.example.smartcustomerservice.domain.dto.TicketResolveRequest;
import com.example.smartcustomerservice.domain.dto.TicketStartRequest;
import com.example.smartcustomerservice.domain.vo.TicketApplySlaVO;
import com.example.smartcustomerservice.domain.vo.TicketAutoAssignPreviewVO;
import com.example.smartcustomerservice.domain.vo.TicketFullDetailVO;
import com.example.smartcustomerservice.domain.vo.TicketOperationLogVO;
import com.example.smartcustomerservice.domain.vo.TicketProcessRecordVO;
import com.example.smartcustomerservice.domain.vo.TicketVO;

// TicketService 属于智能客服平台基础代码。
public interface TicketService {

    TicketVO manualTransfer(Long sessionId, ConversationManualTransferRequest request);

    PageResult<TicketVO> pagePendingReviewTickets(TicketQueryRequest request);

    TicketVO getTicketDetail(Long id);

    TicketFullDetailVO getTicketFullDetail(Long id);

    TicketVO reviewTicket(Long id, TicketReviewRequest request);

    PageResult<TicketVO> pageWaitingAssignTickets(TicketQueryRequest request);

    TicketVO assignTicket(Long id, TicketAssignRequest request);

    TicketAutoAssignPreviewVO previewAutoAssign(Long id);

    TicketVO autoAssignTicket(Long id);

    PageResult<TicketVO> pageMyTickets(TicketQueryRequest request);

    TicketVO startTicket(Long id, TicketStartRequest request);

    TicketProcessRecordVO addTicketProcessRecord(Long id, TicketProcessRecordCreateRequest request);

    PageResult<TicketProcessRecordVO> pageTicketProcessRecords(Long id, TicketProcessRecordQueryRequest request);

    PageResult<TicketOperationLogVO> pageTicketOperationLogs(Long id, TicketOperationLogQueryRequest request);

    TicketVO resolveTicket(Long id, TicketResolveRequest request);

    TicketVO closeTicket(Long id, TicketCloseRequest request);

    TicketApplySlaVO applySla(Long id, TicketApplySlaRequest request);
}
