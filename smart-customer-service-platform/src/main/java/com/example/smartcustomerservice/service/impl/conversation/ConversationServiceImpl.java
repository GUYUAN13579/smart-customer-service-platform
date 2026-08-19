package com.example.smartcustomerservice.service.impl.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.ConversationCloseRequest;
import com.example.smartcustomerservice.domain.dto.ConversationCreateRequest;
import com.example.smartcustomerservice.domain.dto.ConversationMessageCreateRequest;
import com.example.smartcustomerservice.domain.dto.ConversationQueryRequest;
import com.example.smartcustomerservice.domain.dto.ConversationTakeOverRequest;
import com.example.smartcustomerservice.domain.entity.ConversationMessage;
import com.example.smartcustomerservice.domain.entity.ConversationSession;
import com.example.smartcustomerservice.domain.entity.Customer;
import com.example.smartcustomerservice.domain.vo.ConversationMessageVO;
import com.example.smartcustomerservice.domain.vo.ConversationSessionVO;
import com.example.smartcustomerservice.mapper.conversation.ChannelAccountMapper;
import com.example.smartcustomerservice.mapper.conversation.ConversationMessageMapper;
import com.example.smartcustomerservice.mapper.conversation.ConversationSessionMapper;
import com.example.smartcustomerservice.mapper.customer.CustomerMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.conversation.ConversationService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationSessionMapper conversationSessionMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ChannelAccountMapper channelAccountMapper;
    private final CustomerMapper customerMapper;

    public ConversationServiceImpl(ConversationSessionMapper conversationSessionMapper,
                                   ConversationMessageMapper conversationMessageMapper,
                                   ChannelAccountMapper channelAccountMapper,
                                   CustomerMapper customerMapper) {
        this.conversationSessionMapper = conversationSessionMapper;
        this.conversationMessageMapper = conversationMessageMapper;
        this.channelAccountMapper = channelAccountMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationSessionVO createSession(ConversationCreateRequest request) {
        // TODO: 1. 校验 customer 是否存在。
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if(customer == null)
            throw new BusinessException(ResultCode.CUSTOMER_NOT_FOUND, "客户信息不存在");
        // TODO: 2. 生成 sessionNo。
        String sessionId = "S" + customer.getId() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        // TODO: 3. 创建 ConversationSession，默认 status=ACTIVE、aiEnabled 不传则为 1。
        ConversationSession conversationSession = new ConversationSession();
        conversationSession.setCustomerId(customer.getId());
        conversationSession.setSessionNo(sessionId);
        conversationSession.setChannel(request.getChannel());
        if(request.getAiEnabled() == null)
            conversationSession.setAiEnabled(1);
        else
            conversationSession.setAiEnabled(request.getAiEnabled());
        conversationSession.setStatus("ACTIVE");
        // 当前先不绑定人工客服，后续由人工接管或派单逻辑写入 currentAgentId。
        conversationSession.setCurrentAgentId(null);
        // TODO: 5. 插入数据库并转换为 ConversationSessionVO 返回。
        int flag = conversationSessionMapper.insert(conversationSession);
        if(flag == 0)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "创建会话失败");
        ConversationSessionVO conversationSessionVO = new ConversationSessionVO();
        conversationSessionVO.setId(conversationSession.getId());
        conversationSessionVO.setCurrentAgentId(conversationSession.getCurrentAgentId());
        conversationSessionVO.setSessionNo(conversationSession.getSessionNo());
        conversationSessionVO.setCreatedAt(conversationSession.getCreatedAt());
        conversationSessionVO.setChannel(conversationSession.getChannel());
        conversationSessionVO.setAiEnabled(conversationSession.getAiEnabled());
        conversationSessionVO.setUpdatedAt(conversationSession.getUpdatedAt());
        conversationSessionVO.setLastMessageAt(conversationSession.getLastMessageAt());
        conversationSessionVO.setStatus(conversationSession.getStatus());
        conversationSessionVO.setCustomerId(conversationSession.getCustomerId());
        return conversationSessionVO;
    }

    @Override
    public ConversationSessionVO getSession(Long id) {
        if(id == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "id 不存在");
        // TODO: 1. 根据 id 查询 conversation_session。
        ConversationSession conversationSession = conversationSessionMapper.selectById(id);
        // TODO: 2. 不存在则抛 NOT_FOUND。
        if(conversationSession == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "Session 不存在");
        // TODO: 3. 转换为 ConversationSessionVO 返回。
        ConversationSessionVO conversationSessionVO = new ConversationSessionVO();
        conversationSessionVO.setId(conversationSession.getId());
        conversationSessionVO.setCurrentAgentId(conversationSession.getCurrentAgentId());
        conversationSessionVO.setSessionNo(conversationSession.getSessionNo());
        conversationSessionVO.setCreatedAt(conversationSession.getCreatedAt());
        conversationSessionVO.setChannel(conversationSession.getChannel());
        conversationSessionVO.setAiEnabled(conversationSession.getAiEnabled());
        conversationSessionVO.setUpdatedAt(conversationSession.getUpdatedAt());
        conversationSessionVO.setLastMessageAt(conversationSession.getLastMessageAt());
        conversationSessionVO.setStatus(conversationSession.getStatus());
        conversationSessionVO.setCustomerId(conversationSession.getCustomerId());
        return conversationSessionVO;
    }

    @Override
    public PageResult<ConversationSessionVO> pageSessions(ConversationQueryRequest request) {
        Page<ConversationSession> page = new Page<>(request.getPage(), request.getSize());

        Page<ConversationSession> pageResult= conversationSessionMapper.selectPage(page,
                new LambdaQueryWrapper<ConversationSession>()
                        .eq(request.getStatus() != null && !request.getStatus().isBlank(),
                                ConversationSession::getStatus, request.getStatus())
                        .eq(request.getCustomerId() != null,
                                ConversationSession::getCustomerId, request.getCustomerId())
                        .eq(request.getCurrentAgentId() != null,
                                ConversationSession::getCurrentAgentId, request.getCurrentAgentId())
                        .like(request.getKeyword() != null && !request.getKeyword().isBlank(),
                                ConversationSession::getSessionNo, request.getKeyword())
                        .eq(request.getChannel() != null && !request.getChannel().isBlank(),
                                ConversationSession::getChannel, request.getChannel())
                        .orderByDesc(ConversationSession::getLastMessageAt)
                        .orderByDesc(ConversationSession::getUpdatedAt)
                        .orderByDesc(ConversationSession::getId)
                );

        List<ConversationSessionVO> voList = pageResult.getRecords()
                .stream()
                .map(entity -> {
                    ConversationSessionVO vo = new ConversationSessionVO();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .toList();
        return PageResult.of(voList, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationSessionVO takeOverSession(Long id, ConversationTakeOverRequest request) {
        ConversationSession conversationSession = conversationSessionMapper.selectById(id);
        if(conversationSession == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        if ("CLOSED".equals(conversationSession.getStatus())) {
            throw new BusinessException(ResultCode.SESSION_CLOSED, "会话已关闭，不能接管");
        }

        Long agentId = request != null && request.getCurrentAgentId() != null
                ? request.getCurrentAgentId()
                : SecurityUtils.getCurrentUserId();

        if (agentId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前客服信息");
        }

        if ("TAKEN_OVER".equals(conversationSession.getStatus())
                && conversationSession.getCurrentAgentId() != null
                && !Objects.equals(conversationSession.getCurrentAgentId(), agentId)) {
            throw new BusinessException(ResultCode.CONFLICT, "当前会话已被其他客服接管");
        }

        if ("TAKEN_OVER".equals(conversationSession.getStatus())
                && Objects.equals(conversationSession.getCurrentAgentId(), agentId)) {
            return getSession(id);
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = conversationSessionMapper.update(
                null,
                new LambdaUpdateWrapper<ConversationSession>()
                        .eq(ConversationSession::getId, conversationSession.getId())
                        .eq(ConversationSession::getStatus, "ACTIVE")
                        .isNull(ConversationSession::getCurrentAgentId)
                        .set(ConversationSession::getCurrentAgentId, agentId)
                        .set(ConversationSession::getStatus, "TAKEN_OVER")
                        .set(ConversationSession::getLastMessageAt, now)
                        .set(ConversationSession::getUpdatedAt, now)
        );

        if (updated == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "当前会话已被其他客服接管或已关闭");
        }

        insertSystemMessage(id, "客服 " + agentId + " 已接管会话", now);
        return getSession(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationSessionVO releaseTakeOverSession(Long id) {
        ConversationSession conversationSession = conversationSessionMapper.selectById(id);
        if(conversationSession == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        if ("CLOSED".equals(conversationSession.getStatus())) {
            throw new BusinessException(ResultCode.SESSION_CLOSED, "会话已关闭，不能退出接管");
        }

        if (!"TAKEN_OVER".equals(conversationSession.getStatus()) || conversationSession.getCurrentAgentId() == null) {
            return getSession(id);
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前客服信息");
        }
        if (!Objects.equals(conversationSession.getCurrentAgentId(), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能退出自己接管的会话");
        }

        LocalDateTime now = LocalDateTime.now();
        Long agentId = conversationSession.getCurrentAgentId();
        int updated = conversationSessionMapper.update(
                null,
                new LambdaUpdateWrapper<ConversationSession>()
                        .eq(ConversationSession::getId, id)
                        .eq(ConversationSession::getStatus, "TAKEN_OVER")
                        .eq(ConversationSession::getCurrentAgentId, currentUserId)
                        .set(ConversationSession::getCurrentAgentId, null)
                        .set(ConversationSession::getStatus, "ACTIVE")
                        .set(ConversationSession::getLastMessageAt, now)
                        .set(ConversationSession::getUpdatedAt, now)
        );
        if(updated == 0)
            throw new BusinessException(ResultCode.CONFLICT, "当前会话状态已变化，退出接管失败");

        insertSystemMessage(id, "客服 " + agentId + " 已退出接管", now);
        return getSession(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationSessionVO closeSession(Long id, ConversationCloseRequest request) {
        ConversationSession conversationSession = conversationSessionMapper.selectById(id);
        if(conversationSession == null)
            throw new BusinessException(ResultCode.SESSION_NOT_EXIST, "会话不存在");
        if("CLOSED".equals(conversationSession.getStatus()))
            throw new BusinessException(ResultCode.SESSION_CLOSED, "会话已经关闭");

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if ("TAKEN_OVER".equals(conversationSession.getStatus())
                && conversationSession.getCurrentAgentId() != null
                && !Objects.equals(conversationSession.getCurrentAgentId(), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能关闭自己接管的会话");
        }

        LocalDateTime now = LocalDateTime.now();
        String closeReason = request == null ? null : request.getCloseReason();

        conversationSession.setStatus("CLOSED");
        conversationSession.setUpdatedAt(now);
        conversationSession.setLastMessageAt(now);
        conversationSession.setCurrentAgentId(null);
        if(conversationSessionMapper.updateById(conversationSession) == 0)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "关闭会话失败");

        String content = closeReason == null || closeReason.isBlank()
                ? "会话已关闭"
                : "会话已关闭，原因：" + closeReason;
        insertSystemMessage(id, content, now);
        return getSession(id);
    }

    @Override
    public List<ConversationMessageVO> listMessages(Long sessionId) {
        // TODO: 1. 校验会话是否存在。
        if(sessionId == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "SessionId 不存在");
        if(conversationSessionMapper.selectById(sessionId) == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        // TODO: 2. 根据 sessionId 查询 conversation_message。
        // TODO: 3. 按 createdAt、id 正序排序。
        List<ConversationMessage> conversationMessageList = conversationMessageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getSessionId, sessionId)
                        .orderByAsc(ConversationMessage::getCreatedAt)
                        .orderByAsc(ConversationMessage::getId)
        );
        // TODO: 4. 转换为 List<ConversationMessageVO> 返回。
        List<ConversationMessageVO> conversationMessageVOList = conversationMessageList.stream()
                .map(msg -> {
                    ConversationMessageVO conversationMessageVO = new ConversationMessageVO();
                    conversationMessageVO.setId(msg.getId());
                    conversationMessageVO.setMessageType(msg.getMessageType());
                    conversationMessageVO.setConfidence(msg.getConfidence());
                    conversationMessageVO.setContent(msg.getContent());
                    conversationMessageVO.setCreatedAt(msg.getCreatedAt());
                    conversationMessageVO.setIntent(msg.getIntent());
                    conversationMessageVO.setSenderId(msg.getSenderId());
                    conversationMessageVO.setSenderType(msg.getSenderType());
                    conversationMessageVO.setSessionId(msg.getSessionId());
                    return conversationMessageVO;
                }).toList();
        return conversationMessageVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationMessageVO sendMessage(Long sessionId, ConversationMessageCreateRequest request) {
        // TODO: 1. 校验会话是否存在。
        if(sessionId == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "SessionId 不存在");
        ConversationSession conversationSession = conversationSessionMapper.selectById(sessionId);
        if(conversationSession == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        // TODO: 2. 如果会话 status=CLOSED，则不允许发送。
        if("CLOSED".equals(conversationSession.getStatus()))
            throw new BusinessException(ResultCode.SESSION_CLOSED, "会话关闭");
        // TODO: 4. messageType 不传则默认 TEXT。
        LocalDateTime now = LocalDateTime.now();
        ConversationMessage conversationMessage = new ConversationMessage();
        if(request.getMessageType() == null)
            request.setMessageType("TEXT");
        // TODO: 5. 插入 ConversationMessage。
        Long senderId = "AGENT".equals(request.getSenderType()) ? SecurityUtils.getCurrentUserId() : request.getSenderId();
        conversationMessage.setConfidence(request.getConfidence());
        conversationMessage.setMessageType(request.getMessageType());
        conversationMessage.setContent(request.getContent());
        conversationMessage.setSenderId(senderId);
        conversationMessage.setIntent(request.getIntent());
        conversationMessage.setSessionId(sessionId);
        conversationMessage.setSenderType(request.getSenderType());
        conversationMessage.setCreatedAt(now);
        if(conversationMessageMapper.insert(conversationMessage) == 0)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "数据库操作错误");
        // TODO: 6. 更新 ConversationSession.lastMessageAt 和 updatedAt。
        conversationSession.setLastMessageAt(now);
        conversationSession.setUpdatedAt(now);
        conversationSessionMapper.updateById(conversationSession);
        // TODO: 7. 转换为 ConversationMessageVO 返回。
        ConversationMessageVO conversationMessageVO = new ConversationMessageVO();
        conversationMessageVO.setSessionId(conversationMessage.getSessionId());
        conversationMessageVO.setMessageType(conversationMessage.getMessageType());
        conversationMessageVO.setId(conversationMessage.getId());
        conversationMessageVO.setIntent(conversationMessage.getIntent());
        conversationMessageVO.setConfidence(conversationMessage.getConfidence());
        conversationMessageVO.setSenderType(conversationMessage.getSenderType());
        conversationMessageVO.setContent(conversationMessage.getContent());
        conversationMessageVO.setCreatedAt(conversationMessage.getCreatedAt());
        conversationMessageVO.setSenderId(conversationMessage.getSenderId());
        return conversationMessageVO;
    }

    private void insertSystemMessage(Long sessionId, String content, LocalDateTime createdAt) {
        ConversationMessage systemMessage = new ConversationMessage();
        systemMessage.setSessionId(sessionId);
        systemMessage.setSenderType("SYSTEM");
        systemMessage.setMessageType("SYSTEM");
        systemMessage.setContent(content);
        systemMessage.setCreatedAt(createdAt);
        conversationMessageMapper.insert(systemMessage);
    }
}
