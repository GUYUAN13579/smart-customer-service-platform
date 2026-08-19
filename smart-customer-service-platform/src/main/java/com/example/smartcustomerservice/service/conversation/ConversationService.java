package com.example.smartcustomerservice.service.conversation;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.ConversationCloseRequest;
import com.example.smartcustomerservice.domain.dto.ConversationCreateRequest;
import com.example.smartcustomerservice.domain.dto.ConversationMessageCreateRequest;
import com.example.smartcustomerservice.domain.dto.ConversationQueryRequest;
import com.example.smartcustomerservice.domain.dto.ConversationTakeOverRequest;
import com.example.smartcustomerservice.domain.vo.ConversationMessageVO;
import com.example.smartcustomerservice.domain.vo.ConversationSessionVO;

import java.util.List;

public interface ConversationService {

    ConversationSessionVO createSession(ConversationCreateRequest request);

    ConversationSessionVO getSession(Long id);

    PageResult<ConversationSessionVO> pageSessions(ConversationQueryRequest request);

    ConversationSessionVO takeOverSession(Long id, ConversationTakeOverRequest request);

    ConversationSessionVO releaseTakeOverSession(Long id);

    ConversationSessionVO closeSession(Long id, ConversationCloseRequest request);

    List<ConversationMessageVO> listMessages(Long sessionId);

    ConversationMessageVO sendMessage(Long sessionId, ConversationMessageCreateRequest request);
}
