package com.example.smartcustomerservice.service.ai;

import com.example.smartcustomerservice.domain.dto.AiChatRequest;
import com.example.smartcustomerservice.domain.dto.AiAutoReplyRequest;
import com.example.smartcustomerservice.domain.dto.AiSessionSummaryRequest;
import com.example.smartcustomerservice.domain.dto.AiTicketDraftRequest;
import com.example.smartcustomerservice.domain.vo.AiAutoReplyVO;
import com.example.smartcustomerservice.domain.vo.AiChatVO;
import com.example.smartcustomerservice.domain.vo.AiSessionSummaryVO;
import com.example.smartcustomerservice.domain.vo.AiTicketDraftVO;

// AiService 属于智能客服平台基础代码。
public interface AiService {

    AiChatVO chat(AiChatRequest request);

    AiSessionSummaryVO summarizeSession(Long sessionId, AiSessionSummaryRequest request);

    AiAutoReplyVO autoReply(Long sessionId, AiAutoReplyRequest request);

    AiTicketDraftVO generateTicketDraft(Long sessionId, AiTicketDraftRequest request);
}
